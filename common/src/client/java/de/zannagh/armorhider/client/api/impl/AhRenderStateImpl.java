package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.common.GlobalRenderScope;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.RenderScopeContext;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.log.DebugTracer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;

/**
 * State holder for the render-management surface. Not part of the public API; reached through
 * the static methods on {@link de.zannagh.armorhider.client.api.AhRenderManagementApi}.
 */
@ApiStatus.Internal
public final class AhRenderStateImpl {

    private static final EnumSet<GlobalRenderScope> SCOPE_FLAGS = EnumSet.noneOf(GlobalRenderScope.class);
    private static final EnumMap<RenderScope, RenderScopeContext> ACTIVE_SCOPES = new EnumMap<>(RenderScope.class);
    private static String currentPlayerName = "";

    private AhRenderStateImpl() {}

    // --- Phase flags ---

    public static boolean isInLevelRender() {
        return SCOPE_FLAGS.contains(GlobalRenderScope.LEVEL_RENDER);
    }

    public static boolean isInEntityRender() {
        return SCOPE_FLAGS.contains(GlobalRenderScope.ENTITY_RENDER);
    }

    public static void setInLevelRender() {
        DebugTracer.scopeEnterLevelRender();
        clearGlobalScope();
        SCOPE_FLAGS.add(GlobalRenderScope.LEVEL_RENDER);
    }

    public static void exitInLevelRender() {
        DebugTracer.scopeExitLevelRender();
        clearGlobalScope();
    }

    public static void setInEntityRender() {
        DebugTracer.scopeEnterEntityRender();
        clearCurrentPlayer();
        // Reset any per-entity scope state that may have leaked from the previous entity render.
        // Without this, an armor render that's cancelled at @At("HEAD") never sees its @At("RETURN")
        // hook fire, leaving the scope context active for the next frame's body submit.
        ACTIVE_SCOPES.clear();
        SCOPE_FLAGS.add(GlobalRenderScope.ENTITY_RENDER);
    }

    public static void exitEntityRender() {
        DebugTracer.scopeExitEntityRender();
        clearCurrentPlayer();
        ACTIVE_SCOPES.clear();
        SCOPE_FLAGS.remove(GlobalRenderScope.ENTITY_RENDER);
    }

    public static void clearGlobalScope() {
        SCOPE_FLAGS.clear();
        ACTIVE_SCOPES.clear();
        clearCurrentPlayer();
    }

    public static @NonNull String currentlyHandledPlayerName() {
        return currentPlayerName;
    }

    public static void setCurrentPlayer(String playerName) {
        currentPlayerName = playerName;
    }

    public static void clearCurrentPlayer() {
        currentPlayerName = "";
    }

    // --- Per-scope context management ---

    public static @NonNull RenderScopeContext enterScope(RenderScope scope, @Nullable IdentityCarrier carrier,
                                                         @Nullable EquipmentSlot slot, @Nullable ItemStack item) {
        if (carrier == null) {
            var ctx = RenderScopeContext.empty(scope);
            ACTIVE_SCOPES.put(scope, ctx);
            return ctx;
        }

        setCurrentPlayer(carrier.armorHider$playerName());

        SlotModification mod = slot != null
                ? carrier.getModification(slot, item)
                : SlotModification.empty();

        var ctx = new RenderScopeContext(scope, carrier, mod, new RenderModifications(mod));
        ACTIVE_SCOPES.put(scope, ctx);

        if (!mod.isEmpty()) {
            DebugTracer.scopeEnterItemRender(mod.slot(), mod.playerName(), mod.transparency());
        }
        return ctx;
    }

    public static @NonNull RenderScopeContext enterScope(RenderScope scope, SlotModification modification) {
        if (modification == null || modification.isEmpty()) {
            var empty = RenderScopeContext.empty(scope);
            ACTIVE_SCOPES.put(scope, empty);
            return empty;
        }
        var ctx = new RenderScopeContext(scope, null, modification, new RenderModifications(modification));
        ACTIVE_SCOPES.put(scope, ctx);
        setCurrentPlayer(modification.playerName());
        DebugTracer.scopeEnterItemRender(modification.slot(), modification.playerName(), modification.transparency());
        return ctx;
    }

    public static void exitScope(RenderScope scope) {
        ACTIVE_SCOPES.remove(scope);
        DebugTracer.scopeExitItemRender();
    }

    public static @NonNull RenderScopeContext getActiveScope(RenderScope scope) {
        var ctx = ACTIVE_SCOPES.get(scope);
        return ctx != null ? ctx : RenderScopeContext.empty(scope);
    }

    public static boolean hasScopeModification(RenderScope scope) {
        var ctx = ACTIVE_SCOPES.get(scope);
        return ctx != null && !ctx.isEmpty();
    }

    public static boolean shouldEnforceVanillaRendering() {
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderDisabled()) {
            return false;
        }
        String playerName = currentlyHandledPlayerName();
        if (playerName.isBlank()) {
            return false;
        }

        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);

        if (!ArmorHiderClient.CLIENT_CONFIG_MANAGER.shouldApplyCombatDetection(config)) {
            return false;
        }
        if (!ArmorHiderApi.getInstance().getCombatManagement().isInCombat(playerName)) {
            return false;
        }

        return config.inCombatUseDefaultModel.getValue();
    }
}
