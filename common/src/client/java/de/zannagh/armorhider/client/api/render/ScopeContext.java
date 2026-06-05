package de.zannagh.armorhider.client.api.render;

import de.zannagh.armorhider.client.api.configuration.SlotModification;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import org.jspecify.annotations.Nullable;

/**
 * Represents an active render scope with its resolved modification.
 * Returned by {@link ArmorHiderRenderingScopeApi#enterScope} and passed
 * between mixin injection points via {@code @Share}.
 *
 * @since 0.12.0
 */
public record ScopeContext(
        RenderScope scope,
        @Nullable IdentityCarrier carrier,
        SlotModification modification,
        RenderModificationApi renderModificationApi
) {
    public boolean shouldCancel() {
        return modification.shouldHide();
    }

    public boolean needsModification() {
        return modification.needsModification();
    }

    public boolean isEmpty() {
        return modification.isEmpty();
    }

    public static ScopeContext empty(RenderScope scope) {
        return new ScopeContext(scope, null, SlotModification.empty(), RenderModifications.empty());
    }
}
