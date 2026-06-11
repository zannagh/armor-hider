package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.client.api.impl.AhRenderStateImpl;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.RenderScopeContext;
import de.zannagh.armorhider.client.common.SlotModification;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Static entry point for render-scope state.
 * <p>
 * The render pipeline has two levels of state:
 * <ul>
 *   <li><b>Global phase flags</b> ({@link #isInLevelRender()}, {@link #isInEntityRender()}) —
 *       track where in the Minecraft render pipeline we are.</li>
 *   <li><b>Per-scope contexts</b> ({@link #getActiveScope}, {@link #hasScopeModification}) —
 *       each render concern (armor, elytra, cape, offhand, head) has its own isolated context
 *       with the active modification for that scope.</li>
 * </ul>
 * Read methods are part of the public API. The mutator methods ({@code enterScope},
 * {@code exitScope}, phase setters, …) are marked {@link ApiStatus.Internal} because they
 * are only meant to be called from inside the mod's own mixins.
 *
 * @since 0.12.0
 */
@ApiStatus.NonExtendable
public interface AhRenderManagementApi {

    // region Public reads

    /**
     * Whether rendering is currently in the level render phase.
     */
    static boolean isInLevelRender() {
        return AhRenderStateImpl.isInLevelRender();
    }

    /**
     * Whether rendering is currently in the entity render phase.
     */
    static boolean isInEntityRender() {
        return AhRenderStateImpl.isInEntityRender();
    }

    /**
     * The name of the player currently being rendered, or an empty string if no player is being rendered.
     */
    static @NonNull String currentlyHandledPlayerName() {
        return AhRenderStateImpl.currentlyHandledPlayerName();
    }

    /**
     * Query the active context for a specific scope.
     *
     * @return The active scope context, or {@link RenderScopeContext#empty(RenderScope)} if none is active.
     */
    static @NonNull RenderScopeContext getActiveScope(RenderScope scope) {
        return AhRenderStateImpl.getActiveScope(scope);
    }

    /**
     * @return True if the given scope has an active, non-empty modification.
     */
    static boolean hasScopeModification(RenderScope scope) {
        return AhRenderStateImpl.hasScopeModification(scope);
    }

    /**
     * @return whether vanilla rendering should be enforced (combat-detection result). Used by compat
     * code (EMF, …) that needs to fall back to vanilla geometry when a player's combat config requires it.
     */
    static boolean shouldEnforceVanillaRendering() {
        return AhRenderStateImpl.shouldEnforceVanillaRendering();
    }

    // endregion

    // region Internal mutators

    @ApiStatus.Internal
    static void setInLevelRender() {
        AhRenderStateImpl.setInLevelRender();
    }

    @ApiStatus.Internal
    static void exitInLevelRender() {
        AhRenderStateImpl.exitInLevelRender();
    }

    @ApiStatus.Internal
    static void setInEntityRender() {
        AhRenderStateImpl.setInEntityRender();
    }

    @ApiStatus.Internal
    static void exitEntityRender() {
        AhRenderStateImpl.exitEntityRender();
    }

    @ApiStatus.Internal
    static void clearGlobalScope() {
        AhRenderStateImpl.clearGlobalScope();
    }

    @ApiStatus.Internal
    static void setCurrentPlayer(String playerName) {
        AhRenderStateImpl.setCurrentPlayer(playerName);
    }

    @ApiStatus.Internal
    static void clearCurrentPlayer() {
        AhRenderStateImpl.clearCurrentPlayer();
    }

    /**
     * Enter a render scope. Resolves the modification for the given carrier/slot/item and stores it
     * as the active context for this scope. Returns an empty context if the carrier has no
     * modification for this slot.
     */
    @ApiStatus.Internal
    static @NonNull RenderScopeContext enterScope(RenderScope scope, @Nullable IdentityCarrier carrier,
                                                  @Nullable EquipmentSlot slot, @Nullable ItemStack item) {
        return AhRenderStateImpl.enterScope(scope, carrier, slot, item);
    }

    /**
     * Convenience overload: enter a scope from a {@link RenderInterceptionResult} produced by a renderer.
     */
    @ApiStatus.Internal
    static @NonNull RenderScopeContext enterScope(RenderInterceptionResult result) {
        return AhRenderStateImpl.enterScope(result.scope(), result.carrier(), result.getSlot(), result.getItemInfo().getStack());
    }

    /**
     * Enter a scope from an already-resolved modification. Used by compat mixins (Wildfire, …) that
     * have to set up scope state by hand because the normal renderer pipeline didn't run for them.
     */
    @ApiStatus.Internal
    static @NonNull RenderScopeContext enterScope(RenderScope scope, SlotModification modification) {
        return AhRenderStateImpl.enterScope(scope, modification);
    }

    /**
     * Exit a render scope, clearing its context.
     */
    @ApiStatus.Internal
    static void exitScope(RenderScope scope) {
        AhRenderStateImpl.exitScope(scope);
    }

    // endregion
}
