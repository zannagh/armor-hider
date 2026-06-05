package de.zannagh.armorhider.client.common;

import de.zannagh.armorhider.client.api.ArmorHiderRenderApi;import de.zannagh.armorhider.client.api.ArmorHiderRenderModificationApi;
import de.zannagh.armorhider.client.api.implementations.RenderModifications;
import org.jspecify.annotations.Nullable;

/**
 * Represents an active render scope with its resolved modification.
 * Returned by {@link ArmorHiderRenderApi#enterScope} and passed
 * between mixin injection points via {@code @Share}.
 *
 * @since 0.12.0
 */
public record RenderScopeContext(
        RenderScope scope,
        @Nullable IdentityCarrier carrier,
        SlotModification modification,
        ArmorHiderRenderModificationApi renderModificationApi
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

    public static RenderScopeContext empty(RenderScope scope) {
        return new RenderScopeContext(scope, null, SlotModification.empty(), RenderModifications.empty());
    }
}
