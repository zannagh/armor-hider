package de.zannagh.armorhider.client.api.render;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.configuration.SlotModification;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Manages render scopes for the armor hiding pipeline.
 * <p>
 * The render pipeline has two levels of state:
 * <ul>
 *   <li><b>Global Phase flags</b> ({@link #isInLevelRender()}, {@link #isInEntityRender()}) —
 *       track where in the Minecraft render pipeline we are. Used by {@code PlayerMixin}
 *       to decide when to return fake empty items from {@code getItemBySlot}.</li>
 *   <li><b>Per-scope contexts</b> ({@link #enterScope}, {@link #exitScope}) —
 *       each render concern (armor, elytra, cape, offhand, head) has its own isolated
 *       context with the active modification for that scope.</li>
 * </ul>
 *
 * @since 0.12.0
 */
public interface ArmorHiderRenderingScopeApi {

    // region Global phase flags

    /**
     * Global phase flags ({@link GlobalRenderScope}).
     * @return The set of global phase flags.
     */
    EnumSet<GlobalRenderScope> getScopeFlags();

    /**
     * Whether rendering is currently in the level render phase.
     * @return True if rendering is currently in the level render phase.
     */
    default boolean isInLevelRender() {
        return getScopeFlags().contains(GlobalRenderScope.LEVEL_RENDER);
    }

    /**
     * Entering level render resets all scopes, player name, and entity render flag.
     * Exiting clears them as well.
     */
    default void setInLevelRender() {
        clearGlobalScope();
        getScopeFlags().add(GlobalRenderScope.LEVEL_RENDER);
    }

    /**
     * Exiting level render clears all scopes, player name, and entity render flag.
     */
    default void exitInLevelRender() {
        clearGlobalScope();
    }

    /**
     * Whether rendering is currently in the entity render phase.
     * @return True if rendering is currently in the entity render phase.
     */
    default boolean isInEntityRender() {
        return getScopeFlags().contains(GlobalRenderScope.ENTITY_RENDER);
    }

    /**
     * Entering entity render resets all scopes and player name.
     * Exiting clears them as well.
     */
    default void setInEntityRender() {
        clearCurrentPlayer();
        getScopeFlags().add(GlobalRenderScope.ENTITY_RENDER);
    }

    /**
     * Exiting entity render clears all scopes and player name.
     */
    default void exitEntityRender() {
        clearCurrentPlayer();
        getScopeFlags().remove(GlobalRenderScope.ENTITY_RENDER);
    }

    /**
     * Clears all global phase flags and player name.
     */
    default void clearGlobalScope() {
        getScopeFlags().clear();
        clearCurrentPlayer();
    }

    /**
     * The name of the player currently being rendered.
     * @return The player name, or an empty string if no player is being rendered.
     */
    @NonNull String currentlyHandledPlayerName();

    void setCurrentPlayer(String playerName);

    default void clearCurrentPlayer() {
        setCurrentPlayer("");
    }

    // endregion

    // --- Per-scope context management ---

    /**
     * Enter a render scope. Resolves the modification for the given carrier/slot/item
     * and stores it as the active context for this scope.
     *
     * @return The scope context with the resolved modification. Never null.
     *         Returns an empty context if the carrier has no modification for this slot.
     */
    ScopeContext enterScope(RenderScope scope, @Nullable IdentityCarrier carrier,
                            @Nullable EquipmentSlot slot, @Nullable ItemStack item);

    /**
     * Exit a render scope, clearing its context.
     */
    void exitScope(RenderScope scope);

    /**
     * Query the active context for a specific scope.
     *
     * @return The active scope context, or {@link ScopeContext#empty(RenderScope)} if none is active.
     */
    @NonNull ScopeContext getActiveScope(RenderScope scope);

    /**
     * @return True if the given scope has an active, non-empty modification.
     */
    boolean hasScopeModification(RenderScope scope);

    // --- Legacy convenience (scan all scopes) ---

    boolean hasActiveModification();

    boolean hasActiveModificationFor(EquipmentSlot slot);

    @NonNull SlotModification currentlyActiveModification();

    void setActiveModification(SlotModification modification);

    void clearActiveModification();

    boolean shouldEnforceVanillaRendering();

    default RenderModificationApi getModApiFromLocalRefOrScope(RenderScope scope, LocalRef<ScopeContext> context) {
        if (context.get() == null) {
            if (ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(scope).renderModificationApi() != null) {
                context.set(ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(scope));
                return ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(scope).renderModificationApi();
            }
            return RenderModifications.empty();
        }
        return context.get().renderModificationApi();
    }

    default ScopeContext getScopeContextFromLocalRefOrScope(LocalRef<ScopeContext> context, RenderScope scope) {
        if (context.get() == null) {
            var activeScope = ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(scope);
            context.set(activeScope);
            return activeScope;
        }
        return context.get();
    }
}
