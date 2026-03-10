package de.zannagh.armorhider.scopes;

import de.zannagh.armorhider.rendering.RenderDecisions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//? if >= 1.21.4
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//? if < 1.21.4
//import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Central manager for the rendering scope hierarchy.
 * All mixin interaction goes through this class.
 * Scope hierarchy invariant:
 *   RenderFrameScope > LevelRenderScope > EntityRenderScope > ItemRenderScope
 * Entering a higher-level scope automatically clears all deeper scopes
 * (safety net for leaked contexts).
 * Thread safety: each ThreadLocal is independent per-thread.
 */
public final class ScopeProvider {

    private final ThreadLocal<Boolean> renderFrameScope = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<Boolean> levelRenderScope = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<EntityRenderScope> entityRenderScope = new ThreadLocal<>();
    private final ThreadLocal<ItemRenderScope> itemRenderScope = new ThreadLocal<>();

    // --- RenderFrameScope ---

    /** Called from GameRendererMixin at HEAD. */
    public void enterRenderFrame() {
        levelRenderScope.set(false);
        entityRenderScope.remove();
        itemRenderScope.remove();
        renderFrameScope.set(true);
    }

    /** Called from GameRendererMixin at RETURN. */
    public void exitRenderFrame() {
        itemRenderScope.remove();
        entityRenderScope.remove();
        levelRenderScope.set(false);
        renderFrameScope.set(false);
    }

    public boolean isInRenderFrame() {
        return renderFrameScope.get();
    }

    // --- LevelRenderScope ---

    /** Called from GameRendererMixin at renderLevel HEAD. */
    public void enterLevelRender() {
        entityRenderScope.remove();
        itemRenderScope.remove();
        levelRenderScope.set(true);
    }

    /** Called from GameRendererMixin at renderLevel RETURN. */
    public void exitLevelRender() {
        itemRenderScope.remove();
        entityRenderScope.remove();
        levelRenderScope.set(false);
    }

    public boolean isInLevelRender() {
        return levelRenderScope.get();
    }

    // --- EntityRenderScope ---

    /**
     * Called from EntityRenderDispatcherMixin at HEAD.
     * Creates a sentinel scope — we know we're in entity rendering, but don't
     * yet know which entity. Layer mixins will enrich via {@link #enrichEntityScope}.
     */
    public void enterEntityRender() {
        itemRenderScope.remove();
        entityRenderScope.set(EntityRenderScope.SENTINEL);
    }

    /**
     * Enriches the entity scope with identity information.
     * Called from layer mixins when they know the concrete render state or entity.
     */
    //? if >= 1.21.4 {
    public void enrichEntityScope(@NotNull LivingEntityRenderState renderState) {
        var identity = EntityIdentityResolver.resolve(renderState);
        entityRenderScope.set(new EntityRenderScope(identity.playerName(), identity.isPlayer()));
    }
    //?}

    //? if < 1.21.4 {
    /*public void enrichEntityScope(@NotNull LivingEntity entity) {
        var identity = EntityIdentityResolver.resolve(entity);
        entityRenderScope.set(new EntityRenderScope(identity.playerName(), identity.isPlayer()));
    }
    *///?}

    /** Called from EntityRenderDispatcherMixin at RETURN. */
    public void exitEntityRender() {
        itemRenderScope.remove();
        entityRenderScope.remove();
        EntityIdentityResolver.clearIdentityHint();
    }

    public boolean isInEntityRender() {
        return entityRenderScope.get() != null;
    }

    public @Nullable EntityRenderScope entityScope() {
        return entityRenderScope.get();
    }

    // --- ItemRenderScope ---

    /**
     * Enters an item render scope. Called after ScopeFactory creates the scope object.
     */
    public void enterItemRender(@NotNull ItemRenderScope scope) {
        itemRenderScope.set(scope);
    }

    /** Called from layer mixins at RETURN/TAIL. */
    public void exitItemRender() {
        itemRenderScope.remove();
    }

    public boolean hasItemScope() {
        return itemRenderScope.get() != null;
    }

    public boolean hasItemScope(@NotNull EquipmentSlot slot) {
        var scope = itemRenderScope.get();
        return scope != null && scope.slot() == slot;
    }

    public @Nullable ItemRenderScope itemScope() {
        return itemRenderScope.get();
    }
}
