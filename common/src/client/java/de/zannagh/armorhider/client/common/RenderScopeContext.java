package de.zannagh.armorhider.client.common;

import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderModificationApi;
import de.zannagh.armorhider.client.render.RenderModifications;
import org.jspecify.annotations.Nullable;

/**
 * The state held for an active {@link RenderScope}: the resolved modification for the rendered
 * slot, the identity carrier the modification was resolved against, and a pre-bound
 * {@link AhRenderModificationApi} that downstream mixins use to apply the actual visual changes.
 * <p>
 * Returned by the various {@code AhRenderManagementApi.enterScope(...)} overloads (mod-internal
 * use) and looked up by all consumers via
 * {@link AhRenderManagementApi#getActiveScope(RenderScope)}.
 *
 * @param scope                the scope this context belongs to.
 * @param carrier              identity carrier (entity render state, player, …) — may be
 *                             {@code null} for compat paths that hand-build a modification.
 * @param modification         the resolved modification for the scope's slot; empty when no
 *                             modification applies.
 * @param renderModificationApi a pass-through-safe modification API bound to {@code modification};
 *                              callers can use it without first checking emptiness because every
 *                              method short-circuits to the original value when the modification
 *                              is empty.
 * @since 0.12.0
 */
public record RenderScopeContext(
        RenderScope scope,
        @Nullable IdentityCarrier carrier,
        SlotModification modification,
        AhRenderModificationApi renderModificationApi
) {
    /**
     * Whether the underlying mixin should cancel its render call. {@code true} only when the
     * resolved modification has {@link SlotModification#shouldHide()} set — i.e. the player has
     * asked for this slot to be fully hidden.
     */
    public boolean shouldCancel() {
        return modification.shouldHide();
    }

    /**
     * Whether the modification requires any visual changes at all (transparency, glint toggling,
     * hide). Useful to short-circuit downstream render-type / color WrapOps when the slot is
     * configured but happens to be fully opaque + glint-enabled (no visible change needed).
     */
    public boolean needsModification() {
        return modification.needsModification();
    }

    /**
     * Whether the resolved modification is empty (no config applies to this slot for this player).
     * When {@code true}, rendering can be delegated to the vanilla renderer without any changes.
     */
    public boolean isEmpty() {
        return modification.isEmpty();
    }

    /**
     * Returns an empty / pass-through context for a scope — used as the fallback return value
     * when no scope is currently active. All query methods on it return safe defaults
     * ({@link #isEmpty()} {@code true}, {@link #shouldCancel()} {@code false}).
     */
    public static RenderScopeContext empty(RenderScope scope) {
        return new RenderScopeContext(scope, null, SlotModification.empty(), RenderModifications.empty());
    }
}
