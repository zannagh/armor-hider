package de.zannagh.armorhider.client.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Opaque handle for a rule registered through {@link ArmorHiderRenderApi}. Keep the returned
 * instance if the rule ever needs to be removed again; it is only ever equal to itself.
 * <p>
 * Rules are never implemented by consumers - obtain one from a {@code ArmorHiderRenderApi.*When}
 * registration.
 *
 * @since 0.13.0
 */
@ApiStatus.NonExtendable
@ApiStatus.Experimental
public interface AhRenderRule {

    /**
     * The priority this rule was registered at. <b>Lower numeric values are stronger</b>
     * (MC-modding convention, same as {@link AhRenderInterceptionRegistryApi#defaultPriority()}):
     * the strongest matching rule decides outright, and only equal-priority peers can also
     * contribute. See the precedence rules on {@link ArmorHiderRenderApi}.
     *
     * @return the registered priority.
     *
     * @since 0.13.0
     */
    int priority();

    /**
     * Whether this rule is currently registered. Becomes {@code false} after
     * {@link #unregister()}, {@link ArmorHiderRenderApi#unregisterAll(Object)}, or if Armor Hider
     * dropped the rule because its predicate threw on several consecutive evaluations.
     *
     * @return {@code true} while the rule still participates in render decisions.
     *
     * @since 0.13.0
     */
    boolean isRegistered();

    /**
     * Removes this rule. Equivalent to {@link ArmorHiderRenderApi#unregister(AhRenderRule)};
     * calling it twice is a no-op.
     *
     * @since 0.13.0
     */
    void unregister();
}
