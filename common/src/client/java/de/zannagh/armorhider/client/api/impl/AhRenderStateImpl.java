package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhRenderModificationApi;
import de.zannagh.armorhider.client.common.GlobalRenderScope;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.RenderScopeContext;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.log.DebugLogger;
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

    private static final ThreadLocal<EnumSet<GlobalRenderScope>> SCOPE_FLAGS =
            ThreadLocal.withInitial(() -> EnumSet.noneOf(GlobalRenderScope.class));
    private static final ThreadLocal<EnumMap<RenderScope, RenderScopeContext>> ACTIVE_SCOPES =
            ThreadLocal.withInitial(() -> new EnumMap<>(RenderScope.class));
    private static final ThreadLocal<String> CURRENT_PLAYER_NAME = ThreadLocal.withInitial(() -> "");

    private AhRenderStateImpl() {}

    // --- Phase flags ---

    public static boolean isInLevelRender() {
        return SCOPE_FLAGS.get().contains(GlobalRenderScope.LEVEL_RENDER);
    }

    public static boolean isInEntityRender() {
        return SCOPE_FLAGS.get().contains(GlobalRenderScope.ENTITY_RENDER);
    }

    public static void setInLevelRender() {
        DebugTracer.scopeEnterLevelRender();
        clearGlobalScope();
        SCOPE_FLAGS.get().add(GlobalRenderScope.LEVEL_RENDER);
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
        ACTIVE_SCOPES.get().clear();
        SCOPE_FLAGS.get().add(GlobalRenderScope.ENTITY_RENDER);
    }

    public static void exitEntityRender() {
        DebugTracer.scopeExitEntityRender();
        clearCurrentPlayer();
        ACTIVE_SCOPES.get().clear();
        SCOPE_FLAGS.get().remove(GlobalRenderScope.ENTITY_RENDER);
    }

    public static void clearGlobalScope() {
        if (SCOPE_FLAGS.get().size() <= 1 && ACTIVE_SCOPES.get().isEmpty()) {
            clearCurrentPlayer();
            DebugLogger.log("--- Global scope cleared ---");
            return;
        }
        DebugLogger.log("--- Clearing global scopes ---");
        DebugLogger.log("Remaining scope flags: {}. Remaining uncleared active scopes: {}", SCOPE_FLAGS.get().size(), ACTIVE_SCOPES.get().size());
        SCOPE_FLAGS.get().clear();
        ACTIVE_SCOPES.get().clear();
        clearCurrentPlayer();
        DebugLogger.log("--- Global scope cleared ---");
    }

    public static @NonNull String currentlyHandledPlayerName() {
        return CURRENT_PLAYER_NAME.get();
    }

    public static void setCurrentPlayer(@Nullable String playerName) {
        CURRENT_PLAYER_NAME.set(playerName == null ? "" : playerName);
    }

    public static void clearCurrentPlayer() {
        CURRENT_PLAYER_NAME.set("");
    }

    // --- Per-scope context management ---

    public static @NonNull RenderScopeContext enterScope(RenderScope scope, @Nullable IdentityCarrier carrier,
                                                         @Nullable EquipmentSlot slot, @Nullable ItemStack item) {
        if (carrier == null) {
            var ctx = RenderScopeContext.empty(scope);
            ACTIVE_SCOPES.get().put(scope, ctx);
            DebugTracer.scopeEntered(scope.name(), null, false);
            return ctx;
        }

        String identity = carrier.armorHider$playerName();
        setCurrentPlayer(identity);

        SlotModification mod = slot != null
                ? carrier.getModification(slot, item)
                : SlotModification.empty();

        var ctx = new RenderScopeContext(scope, carrier, mod, AhRenderModificationApi.getInstance(mod));
        ACTIVE_SCOPES.get().put(scope, ctx);

        DebugTracer.scopeEntered(scope.name(), identity, !mod.isEmpty());
        if (!mod.isEmpty()) {
            DebugTracer.scopeEnterItemRender(mod.slot(), mod.playerName(), mod.transparency());
        }
        return ctx;
    }

    public static @NonNull RenderScopeContext enterScope(RenderScope scope, SlotModification modification) {
        if (modification == null || modification.isEmpty()) {
            var empty = RenderScopeContext.empty(scope);
            ACTIVE_SCOPES.get().put(scope, empty);
            DebugTracer.scopeEntered(scope.name(), modification != null ? modification.playerName() : null, false);
            return empty;
        }
        var ctx = new RenderScopeContext(scope, null, modification, AhRenderModificationApi.getInstance(modification));
        ACTIVE_SCOPES.get().put(scope, ctx);
        setCurrentPlayer(modification.playerName());
        DebugTracer.scopeEntered(scope.name(), modification.playerName(), true);
        DebugTracer.scopeEnterItemRender(modification.slot(), modification.playerName(), modification.transparency());
        return ctx;
    }

    public static void exitScope(RenderScope scope) {
        var prev = ACTIVE_SCOPES.get().remove(scope);
        if (prev == null) {
            return;
        }
        String identity = prev.carrier() != null ? prev.carrier().armorHider$playerName() : CURRENT_PLAYER_NAME.get();
        DebugTracer.scopeExited(scope.name(), identity);
        if (!prev.isEmpty()) {
            DebugTracer.scopeExitItemRender();
        }
    }

    public static @NonNull RenderScopeContext getActiveScope(RenderScope scope) {
        var ctx = ACTIVE_SCOPES.get().get(scope);
        return ctx != null ? ctx : RenderScopeContext.empty(scope);
    }

    public static boolean hasScopeModification(RenderScope scope) {
        var ctx = ACTIVE_SCOPES.get().get(scope);
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
