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
 * <b>Ordering:</b> {@link #priority(int)}, {@link #owner(Object)} and {@link #shared()} must come
 * <em>before</em> the effect, because the effect hands off to {@link AhRenderRuleCondition} and there is no way back -
 * {@code rule(HEAD).hide().priority(5)} does not compile. A builder is safe to keep and reuse for
 * several rules: each effect call snapshots the current target, priority, owner and shared flag, so a
 * later registration never inherits an earlier one's effect.
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
     * Marks this rule as <b>shared</b>: what it resolves to <em>for the local player</em> is sent to
     * the server and relayed to every other client, so the other players see the same thing the rule
     * does here. Off by default.
     *
     * <h4>Why it is opt-in</h4>
     * A plain rule is a <b>viewer-side</b> statement - it applies to every player this client renders
     * ("fade everyone's armor while they sprint"). Broadcasting that would push a viewer's own
     * preference onto everyone else's screen. {@code shared()} declares the other kind of rule - a
     * statement about <em>me</em> ("my helmet is off while I am sleeping") - which is the only kind
     * that makes sense to send. A shared rule still evaluates locally exactly as before; sharing is
     * purely additive.
     *
     * <h4>What actually travels</h4>
     * Predicates are arbitrary code and cannot be serialised. The local client evaluates the shared
     * rules against itself and sends the <em>outcome</em> (opacity and glint per target) whenever it
     * changes, so predicates keep running in exactly one place. Consequences worth knowing:
     * <ul>
     *   <li>The condition is evaluated against the <b>local player</b> for this purpose, with the
     *       local player's own stack, config and pre-rule opacity in the {@link AhHideContext}.</li>
     *   <li>Other clients see the change one server round-trip later, and repeated changes are
     *       coalesced, so this is not the tool for a per-frame flicker.</li>
     *   <li>Nothing is sent to a server that does not run Armor Hider (vanilla, or a server without
     *       the plugin): the outgoing packet gate suppresses it, and the rule stays local-only.</li>
     *   <li>A receiving client applies the outcome through its own rule pipeline, so all the usual
     *       limits still hold there - it is skipped wherever that viewer's Armor Hider renders
     *       vanilla, and a local rule with a stronger priority still wins. A remote outcome
     *       participates at {@link ArmorHiderRenderApi#defaultPriority()}.</li>
     *   <li>Every client needs Armor Hider for this to be visible, but only the <em>owning</em> client
     *       needs the mod that registered the rule.</li>
     * </ul>
     *
     * @return this builder.
     * @since 0.13.0
     */
    @NonNull AhRenderRuleBuilder shared();

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
