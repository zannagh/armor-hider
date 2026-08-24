package de.zannagh.armorhider.client.api;

import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Full-control rule construction, started by {@link ArmorHiderRenderApi#rule(EquipmentSlot)} or
 * {@link ArmorHiderRenderApi#elytraRule()}. The target is fixed by the factory method, so a rule can
 * never silently point somewhere other than where it reads.
 * <p>
 * Choosing an effect ({@link #hide()}, {@link #opacity(float)}, {@link #disableGlint()}) moves to
 * {@link AhRenderRuleCondition}, whose {@code when} methods register the rule and hand back the
 * handle. The staging is deliberate: a rule with no target, no effect or no condition does not
 * compile, so there is nothing to validate at runtime.
 * <p>
 * This builder is the <em>only</em> place priority can be set - the convenience methods on
 * {@link ArmorHiderRenderApi} deliberately have no priority overloads, so a bare {@code 0} literal
 * can never be read as a priority where an opacity was meant, or the reverse.
 * <p>
 * <b>Ordering:</b> {@link #priority(int)} and {@link #owner(Object)} must come <em>before</em> the
 * effect, because the effect hands off to {@link AhRenderRuleCondition} and there is no way back -
 * {@code rule(HEAD).hide().priority(5)} does not compile. A builder is safe to keep and reuse for
 * several rules: each effect call snapshots the current target, priority and owner, so a later
 * registration never inherits an earlier one's effect.
 *
 * @since 0.13.0
 */
@ApiStatus.NonExtendable
@ApiStatus.Experimental
public interface AhRenderRuleBuilder {

    /**
     * Sets the evaluation priority. <b>Lower numeric values are stronger</b>, matching the rest of
     * this codebase ({@link AhRenderInterceptionRegistryApi#defaultPriority()}). Defaults to
     * {@link ArmorHiderRenderApi#defaultPriority()}.
     *
     * @return this builder.
     * @since 0.13.0
     */
    @NonNull AhRenderRuleBuilder priority(int priority);

    /**
     * Tags the rule with an owner token (your mod instance, a mod id string, ...) so it can be
     * dropped in bulk via {@link ArmorHiderRenderApi#unregisterAll(Object)}. Owners are compared
     * with {@link Object#equals(Object)}.
     *
     * @return this builder.
     * @since 0.13.0
     */
    @NonNull AhRenderRuleBuilder owner(@Nullable Object owner);

    /**
     * Hides the target entirely. Shorthand for {@code opacity(0f)}.
     *
     * @since 0.13.0
     */
    @NonNull AhRenderRuleCondition hide();

    /**
     * Renders the target at this opacity, {@code 0} invisible to {@code 1} opaque; values outside
     * are clamped. Note that anything below Armor Hider's smallest opacity step (0.05) is a full
     * hide, not a very faint render.
     *
     * @since 0.13.0
     */
    @NonNull AhRenderRuleCondition opacity(float opacity);

    /**
     * Suppresses the enchantment glint, leaving opacity alone.
     *
     * @since 0.13.0
     */
    @NonNull AhRenderRuleCondition disableGlint();
}
