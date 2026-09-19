package de.zannagh.armorhider.client.common;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderModificationApi;
import de.zannagh.armorhider.client.api.AhRenderTypeFactory;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.client.render.rendertype.RenderTypeFactory;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

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
 * @param carrier              identity carrier (entity render state, player, …) - may be
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
     * resolved modification has {@link SlotModification#shouldHide()} set - i.e. the player has
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
     * Whether the piece is genuinely being made translucent (opacity below ~100%), as opposed to
     * {@link #needsModification()} which is also true when only the glint is toggled off at full
     * opacity. Render-type swaps onto the depth-write-disabled translucent pipeline must gate on this,
     * so a solid piece with its glint merely off stays solid (and does not read as see-through under
     * shaders). See {@link SlotModification#needsTranslucency()}.
     */
    public boolean needsTranslucency() {
        return modification.needsTranslucency();
    }

    /**
     * Whether the resolved modification is empty (no config applies to this slot for this player).
     * When {@code true}, rendering can be delegated to the vanilla renderer without any changes.
     */
    public boolean isEmpty() {
        return modification.isEmpty();
    }

    /**
     * Returns an empty / pass-through context for a scope - used as the fallback return value
     * when no scope is currently active. All query methods on it return safe defaults
     * ({@link #isEmpty()} {@code true}, {@link #shouldCancel()} {@code false}).
     */
    public static RenderScopeContext empty(RenderScope scope) {
        Objects.requireNonNull(scope, "scope must not be null when asking for an empty RenderScopeContext");
        RenderScopeContext cached = EMPTY_BY_SCOPE[scope.ordinal()];
        if (cached != null) {
            return cached;
        }
        // Degraded path - see buildEmpties(). Allocates, but keeps rendering alive.
        return new RenderScopeContext(scope, null, SlotModification.empty(), RenderModifications.empty());
    }

    /**
     * One cached empty context per scope. This is the return value of every {@code getActiveScope(...)}
     * MISS - the overwhelmingly common case, queried per model part and per baked quad - and building one
     * used to allocate a {@link SlotModification} plus a {@link RenderModifications}, each carrying a fresh
     * {@code PlayerConfig}. The contents are immutable pass-throughs (see {@code RenderModifications.empty()}),
     * so a single instance per scope is safe to hand out to every caller and thread.
     */
    private static final RenderScopeContext[] EMPTY_BY_SCOPE = buildEmpties();

    /**
     * Builds the cache, leaving entries null if anything on the chain is not ready yet. This mirrors the
     * resilience {@code SlotModification.empty(...)} has for the same reason, and the precedent documented
     * at {@code ItemInfo.java:40-48} (issue #260): this class can be touched during the window where item
     * registries are still binding, and letting a transient failure escape a {@code <clinit>} would latch
     * it for the whole session ({@code ExceptionInInitializerError}, then {@code NoClassDefFoundError}),
     * killing the render path. A null entry only costs an allocation per call until the next restart.
     */
    private static RenderScopeContext[] buildEmpties() {
        RenderScope[] scopes = RenderScope.values();
        RenderScopeContext[] empties = new RenderScopeContext[scopes.length];
        try {
            for (RenderScope scope : scopes) {
                empties[scope.ordinal()] =
                        new RenderScopeContext(scope, null, SlotModification.empty(), RenderModifications.empty());
            }
        } catch (Throwable t) {
            ArmorHider.LOGGER.warn(
                    "Could not pre-build the empty RenderScopeContext cache; falling back to allocating them per call",
                    t);
        }
        return empties;
    }
}
