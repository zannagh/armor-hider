package de.zannagh.armorhider.client.api;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.client.api.impl.AhRendererRegistryImpl;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.interceptors.ArmorHiderEmptyRenderer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Registry of {@link AhRenderer}s keyed by {@link RenderScope}.
 * <p>
 * <b>Priority semantics:</b> lower numeric priority takes precedence (MC-modding convention).
 * The renderer with the lowest registered priority value for a scope wins; ties resolve in
 * registration order. {@link #defaultPriority()} is the value used by the built-in default
 * renderers — register at a lower number to override.
 * <p>
 * <b>{@link RenderScope#ALL} fallback:</b> a renderer registered with target scope {@code ALL}
 * is consulted only when no renderer is registered for the requested specific scope. It acts as
 * a catch-all default, not a chained pre/post hook.
 * <p>
 * Example — install a custom cape renderer that takes precedence over the built-in default:
 * <pre>{@code
 * AhRenderInterceptionRegistryApi.register(
 *     new MyCapeRenderer(),                            // implements AhRenderer, targets CAPE
 *     AhRenderInterceptionRegistryApi.defaultPriority() - 1);
 * }</pre>
 *
 * @since 0.12.0
 */
@ApiStatus.NonExtendable
public interface AhRenderInterceptionRegistryApi {

    /**
     * @return the priority used by all built-in default renderers. Register at a lower number to
     * take precedence; at a higher number to act only as a fallback when the default has been
     * unregistered.
     */
    static int defaultPriority() {
        return AhRendererRegistryImpl.DEFAULT_PRIORITY;
    }

    /**
     * Register {@code renderer} for its declared target scope (see
     * {@link AhRenderer#getTargetScope()}) at the given priority.
     * <p>
     * Multiple renderers may be registered for the same scope; lookups via
     * {@link #getRenderer(RenderScope)} return the one with the lowest priority value.
     * Re-registering the same renderer at a new priority is allowed and adds a second entry —
     * call {@link #unregister(AhRenderer, int)} for the old (priority, renderer) pair first if
     * that is not what you want.
     */
    static void register(AhRenderer renderer, int priority) {
        AhRendererRegistryImpl.register(renderer, priority);
    }

    static void register(AhRenderer renderer) {
        AhRendererRegistryImpl.register(renderer, defaultPriority());
    }

    /**
     * Remove the (priority, renderer) entry matching this exact priority and identity. No-op if
     * the renderer was never registered at that priority.
     */
    static void unregister(AhRenderer renderer, int priority) {
        AhRendererRegistryImpl.unregister(renderer, priority);
    }

    /**
     * Registers a conditional suppressor to replace the actual instance of the renderer with an empty renderer
     * whenever the evaluation returns true (since renderes are resolved via RenderScope).
     * @param evaluation The evaluation to apply.
     */
    static void suppressRenderInterceptionConditionally(RenderScope scope, Function<Pair<RenderScope, AhRenderer>, Boolean> evaluation) {
        AhRendererRegistryImpl.suppressConditionally(scope, evaluation);
    }

    /**
     * Look up the active renderer for a scope. Resolution order: lowest-priority renderer
     * registered for {@code scope} → lowest-priority renderer registered for {@link RenderScope#ALL}
     * → a no-op empty renderer (never {@code null}).
     */
    static AhRenderer getRenderer(RenderScope scope) {
        return AhRendererRegistryImpl.getRenderer(scope);
    }

    /**
     * Look up a registered renderer by its concrete class, ignoring scope. Returns the first
     * renderer whose runtime class is exactly {@code type} (not a subclass), or {@code null}
     * if none is registered.
     * <p>
     * Use when calling code needs a specific implementation rather than the scope-resolved
     * renderer — e.g. resolving a third-party compat interceptor like
     * {@code GeckoLibRenderInterceptor} that registers under {@link RenderScope#ALL} but is
     * addressed by type at the call site.
     */
    static <T extends AhRenderer> @Nullable T getRenderer(Class<T> type) {
        return AhRendererRegistryImpl.getRenderer(type);
    }
}
