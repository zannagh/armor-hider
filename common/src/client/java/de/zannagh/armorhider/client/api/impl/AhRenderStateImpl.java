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
import java.util.concurrent.atomic.AtomicLong;

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

    // Diagnostic counter: scope entries that carried a real (non-empty) modification.
    // The render hooks fail silently when their injection targets drift between MC
    // versions — smoke tests assert this counter moves to catch dead pipelines.
    private static final EnumMap<RenderScope, AtomicLong> MODIFIED_ENTER_COUNTS = new EnumMap<>(RenderScope.class);

    // Count of scopes still active in the map when a bulk clear runs (entity-boundary reset or
    // global-scope clear). Each per-scope enter is normally matched by an exitScope; anything left
    // for the bulk clear to sweep up leaked — it was entered on a render path cancelled before its
    // exit hook could run (e.g. an elytra hidden at 0%). That leaked hide-scope then bleeds alpha 0
    // onto later model submits. Smoke asserts this stays 0. (A plain enter/exit *count* can't be
    // used: ARMOR_PIECE is legitimately entered twice per piece — renderArmorPiece wraps the nested
    // renderLayers — and the single-entry map self-corrects, so counts read 2:1 without any leak.)
    private static final EnumMap<RenderScope, AtomicLong> LEAKED_SCOPE_CLEARS = new EnumMap<>(RenderScope.class);

    static {
        for (RenderScope scope : RenderScope.values()) {
            MODIFIED_ENTER_COUNTS.put(scope, new AtomicLong());
            LEAKED_SCOPE_CLEARS.put(scope, new AtomicLong());
        }
    }

    public static long modifiedScopeEnterCount(RenderScope scope) {
        return MODIFIED_ENTER_COUNTS.get(scope).get();
    }

    public static long leakedScopeClears(RenderScope scope) {
        return LEAKED_SCOPE_CLEARS.get(scope).get();
    }

    private static void armorHider$recordLeakedScopes() {
        var active = ACTIVE_SCOPES.get();
        if (active.isEmpty()) {
            return;
        }
        for (RenderScope scope : active.keySet()) {
            LEAKED_SCOPE_CLEARS.get(scope).incrementAndGet();
        }
    }

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
        armorHider$recordLeakedScopes();
        ACTIVE_SCOPES.get().clear();
        SCOPE_FLAGS.get().add(GlobalRenderScope.ENTITY_RENDER);
    }

    public static void exitEntityRender() {
        DebugTracer.scopeExitEntityRender();
        clearCurrentPlayer();
        armorHider$recordLeakedScopes();
        ACTIVE_SCOPES.get().clear();
        SCOPE_FLAGS.get().remove(GlobalRenderScope.ENTITY_RENDER);
    }

    public static void clearGlobalScope() {
        if (SCOPE_FLAGS.get().size() <= 1 && ACTIVE_SCOPES.get().isEmpty()) {
            // The expected case: at most our own flag remains — but it MUST still be removed,
            // otherwise isInLevelRender() stays true during game ticks and slot hiding leaks
            // into gameplay checks (elytra takeoff, shield blocking).
            SCOPE_FLAGS.get().clear();
            clearCurrentPlayer();
            DebugLogger.log("--- Global scope cleared ---");
            return;
        }
        DebugLogger.log("--- Clearing global scopes ---");
        DebugLogger.log("Remaining scope flags: {}. Remaining uncleared active scopes: {}", SCOPE_FLAGS.get().size(), ACTIVE_SCOPES.get().size());
        armorHider$recordLeakedScopes();
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
            MODIFIED_ENTER_COUNTS.get(scope).incrementAndGet();
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
        MODIFIED_ENTER_COUNTS.get(scope).incrementAndGet();
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
