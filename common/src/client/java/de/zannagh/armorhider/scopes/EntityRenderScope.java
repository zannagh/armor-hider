package de.zannagh.armorhider.scopes;

import org.jetbrains.annotations.Nullable;
//? if >= 1.21.4
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//? if < 1.21.4
//import net.minecraft.world.entity.LivingEntity;

/**
 * Represents the scope of rendering a single entity.
 * Active during EntityRenderDispatcher.render()/submit().
 *
 * Holds the entity identity information needed to resolve player configs.
 * The version-specific identity resolution is delegated to {@link EntityIdentityResolver}.
 *
 * Immutable value object — the SENTINEL is used when we know we're in entity
 * rendering but haven't resolved identity yet (EntityRenderDispatcherMixin enters
 * before layer mixins can determine which entity).
 */
public final class EntityRenderScope {

    /**
     * Sentinel indicating we are inside entity rendering but haven't
     * resolved identity yet. {@link ScopeProvider#isInEntityRender()} returns true,
     * but {@link #resolvedPlayerName()} returns null.
     */
    public static final EntityRenderScope SENTINEL = new EntityRenderScope(null, false);

    private final @Nullable String resolvedPlayerName;
    private final boolean isPlayerEntity;

    public EntityRenderScope(@Nullable String resolvedPlayerName, boolean isPlayerEntity) {
        this.resolvedPlayerName = resolvedPlayerName;
        this.isPlayerEntity = isPlayerEntity;
    }

    public @Nullable String resolvedPlayerName() { return resolvedPlayerName; }
    public boolean isPlayerEntity() { return isPlayerEntity; }
}
