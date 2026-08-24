package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.client.api.impl.AhRenderRuleBuilderImpl;
import de.zannagh.armorhider.client.api.impl.AhRenderRuleRegistryImpl;
import de.zannagh.armorhider.client.api.impl.AhRuleTarget;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Ergonomic entry point for third-party mods that want to hide, fade or de-glint equipment on
 * their own conditions - without implementing {@link AhRenderer}, writing a mixin, or touching
 * the interception pipeline. Register a predicate, keep the returned {@link AhRenderRule},
 * unregister it when your mod shuts the feature off.
 *
 * <pre>{@code
 * AhRenderRule rule = ArmorHiderRenderApi.hideArmorWhen(
 *         EquipmentSlot.HEAD, Player::isSleeping);
 * }</pre>
 *
 * <h2>Two predicate flavours</h2>
 * Every registration exists twice: the plain form takes a {@code Predicate<Player>}, the
 * {@code *Matching} form takes a {@code Predicate<}{@link AhHideContext}{@code >} and additionally
 * sees the slot, the stack, the resolved config and the pre-rule opacity. A {@code Predicate<Player>}
 * rule does <b>not</b> match while the entity cannot be resolved on the client (out of render
 * distance, already removed) - use the {@code *Matching} form if the name alone is enough.
 *
 * <h2>Precedence</h2>
 * <ul>
 *   <li><b>Priority decides.</b> The strongest matching rule wins outright: only rules at the
 *       strongest priority present contribute. <b>Lower numeric values are stronger</b>, matching
 *       the rest of this codebase - see {@link #defaultPriority()}.</li>
 *   <li><b>Ties hide most.</b> Among equal-priority matching rules the lowest opacity wins.</li>
 *   <li>The winning opacity <em>replaces</em> the value Armor Hider derived from the user's config
 *       (combat detection included), so a rule can also make a piece more visible than the user
 *       configured.</li>
 *   <li><b>Glint ignores priority entirely.</b> Priority bands opacity only - any matching glint
 *       rule suppresses the glint whatever its priority, and nothing can force it back on. The
 *       asymmetry is deliberate: "disable" is glint's only direction, so banding it would just let
 *       an unrelated opacity rule swallow another mod's glint rule.</li>
 * </ul>
 * Priority is settable only through {@link #rule(EquipmentSlot)} / {@link #elytraRule()}; the
 * convenience methods below have deliberately no priority overloads, so a bare {@code 0} can never
 * be mistaken between a priority and an opacity argument.
 *
 * <h2>Limits worth knowing</h2>
 * Rules live inside Armor Hider's own decision path, so they are skipped wherever Armor Hider
 * deliberately renders vanilla (global kill switch, "disable on others", per-player disable,
 * excluded items, skulls with "opacity affects hats/skulls" off). An opacity below Armor Hider's
 * smallest step (0.05) is a full hide, not a faint render. Predicates are evaluated exactly once
 * per player, per slot, per modification, on the render thread - keep them cheap. A predicate that
 * throws counts as non-matching and is logged at most once per minute per rule; only after five
 * consecutive throws is the rule dropped, and any successful evaluation resets that streak. See
 * {@code readme.md} in this package for the full cookbook.
 *
 * @since 0.13.0
 */
@ApiStatus.NonExtendable
@ApiStatus.Experimental
public interface ArmorHiderRenderApi {

    /**
     * @return the priority every convenience registration uses. Lower is stronger, so register
     * below this to outrank the defaults.
     * @since 0.13.0
     */
    static int defaultPriority() {
        return AhRenderRuleRegistryImpl.DEFAULT_PRIORITY;
    }

    /**
     * Starts a fully controlled rule for an equipment slot - the only way to set a priority or an
     * owner. Elytra wings are a separate target, see {@link #elytraRule()}.
     *
     * @param slot the slot to target.
     * @return the builder; the rule is registered by the terminal {@code when} call.
     * @throws IllegalArgumentException immediately if Armor Hider does not render {@code slot} -
     *         {@code MAINHAND}, {@code BODY} and {@code SADDLE}. Rejected rather than accepted-and-
     *         ignored, so a rule that could never fire fails loudly at registration instead of
     *         silently doing nothing. Registration-time programming error, never per-frame.
     * @since 0.13.0
     */
    static @NonNull AhRenderRuleBuilder rule(@NonNull EquipmentSlot slot) {
        AhRuleTarget target = AhRuleTarget.of(slot);
        if (target == null) {
            throw new IllegalArgumentException("Armor Hider does not render equipment slot " + slot);
        }
        return new AhRenderRuleBuilderImpl(target);
    }

    /**
     * Starts a fully controlled rule for elytra wings. Separate from {@link #rule(EquipmentSlot)}
     * because the wings are worn in the chest slot but follow their own opacity and glint settings.
     *
     * @return the builder; the rule is registered by the terminal {@code when} call.
     * @since 0.13.0
     */
    static @NonNull AhRenderRuleBuilder elytraRule() {
        return new AhRenderRuleBuilderImpl(AhRuleTarget.ELYTRA);
    }

    /**
     * Hides the armor piece in {@code slot} whenever {@code condition} matches. Plain elytra wings
     * are not covered - see {@link #hideElytraWhen(Predicate)}.
     *
     * @return the handle to unregister this rule with.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule hideArmorWhen(@NonNull EquipmentSlot slot, @NonNull Predicate<Player> condition) {
        return rule(slot).hide().when(condition);
    }

    /**
     * Hides elytra wings whenever {@code condition} matches. The elytra is independent of the chest
     * slot, so hiding {@link EquipmentSlot#CHEST} does not hide the wings. An <em>armored</em>
     * elytra is the exception - it follows the chest rules, see the readme.
     *
     * @return the handle to unregister this rule with.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule hideElytraWhen(@NonNull Predicate<Player> condition) {
        return elytraRule().hide().when(condition);
    }

    /**
     * Hides the off-hand item whenever {@code condition} matches.
     *
     * @return the handle to unregister this rule with.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule hideOffhandWhen(@NonNull Predicate<Player> condition) {
        return hideArmorWhen(EquipmentSlot.OFFHAND, condition);
    }

    /**
     * Renders the piece in {@code slot} at {@code opacity} - {@code 0} invisible to {@code 1}
     * opaque, clamped - whenever {@code condition} matches.
     *
     * @return the handle to unregister this rule with.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule setOpacityWhen(@NonNull EquipmentSlot slot, float opacity, @NonNull Predicate<Player> condition) {
        return rule(slot).opacity(opacity).when(condition);
    }

    /**
     * Renders elytra wings at {@code opacity} - {@code 0} invisible to {@code 1} opaque, clamped -
     * whenever {@code condition} matches.
     *
     * @return the handle to unregister this rule with.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule setElytraOpacityWhen(float opacity, @NonNull Predicate<Player> condition) {
        return elytraRule().opacity(opacity).when(condition);
    }

    /**
     * Renders the off-hand item at {@code opacity} - {@code 0} invisible to {@code 1} opaque,
     * clamped - whenever {@code condition} matches.
     *
     * @return the handle to unregister this rule with.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule setOffhandOpacityWhen(float opacity, @NonNull Predicate<Player> condition) {
        return setOpacityWhen(EquipmentSlot.OFFHAND, opacity, condition);
    }

    /**
     * Suppresses the enchantment glint on the piece in {@code slot} whenever {@code condition}
     * matches, leaving its opacity alone.
     *
     * @return the handle to unregister this rule with.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule disableGlintWhen(@NonNull EquipmentSlot slot, @NonNull Predicate<Player> condition) {
        return rule(slot).disableGlint().when(condition);
    }

    /**
     * Suppresses the enchantment glint on elytra wings whenever {@code condition} matches.
     *
     * @return the handle to unregister this rule with.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule disableElytraGlintWhen(@NonNull Predicate<Player> condition) {
        return elytraRule().disableGlint().when(condition);
    }

    /**
     * Context-aware {@link #hideArmorWhen(EquipmentSlot, Predicate)}.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule hideArmorWhenMatching(@NonNull EquipmentSlot slot, @NonNull Predicate<AhHideContext> condition) {
        return rule(slot).hide().whenMatching(condition);
    }

    /**
     * Context-aware {@link #hideElytraWhen(Predicate)}.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule hideElytraWhenMatching(@NonNull Predicate<AhHideContext> condition) {
        return elytraRule().hide().whenMatching(condition);
    }

    /**
     * Context-aware {@link #hideOffhandWhen(Predicate)}.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule hideOffhandWhenMatching(@NonNull Predicate<AhHideContext> condition) {
        return hideArmorWhenMatching(EquipmentSlot.OFFHAND, condition);
    }

    /**
     * Context-aware {@link #setOpacityWhen(EquipmentSlot, float, Predicate)}.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule setOpacityWhenMatching(@NonNull EquipmentSlot slot, float opacity, @NonNull Predicate<AhHideContext> condition) {
        return rule(slot).opacity(opacity).whenMatching(condition);
    }

    /**
     * Context-aware {@link #setElytraOpacityWhen(float, Predicate)}.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule setElytraOpacityWhenMatching(float opacity, @NonNull Predicate<AhHideContext> condition) {
        return elytraRule().opacity(opacity).whenMatching(condition);
    }

    /**
     * Context-aware {@link #setOffhandOpacityWhen(float, Predicate)}.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule setOffhandOpacityWhenMatching(float opacity, @NonNull Predicate<AhHideContext> condition) {
        return setOpacityWhenMatching(EquipmentSlot.OFFHAND, opacity, condition);
    }

    /**
     * Context-aware {@link #disableGlintWhen(EquipmentSlot, Predicate)}.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule disableGlintWhenMatching(@NonNull EquipmentSlot slot, @NonNull Predicate<AhHideContext> condition) {
        return rule(slot).disableGlint().whenMatching(condition);
    }

    /**
     * Context-aware {@link #disableElytraGlintWhen(Predicate)}.
     * @since 0.13.0
     */
    static @NonNull AhRenderRule disableElytraGlintWhenMatching(@NonNull Predicate<AhHideContext> condition) {
        return elytraRule().disableGlint().whenMatching(condition);
    }

    /**
     * Removes a single rule. No-op for {@code null} or an already-removed rule.
     * @since 0.13.0
     */
    static void unregister(@Nullable AhRenderRule rule) {
        AhRenderRuleRegistryImpl.unregister(rule);
    }

    /**
     * Removes every rule tagged with this {@code owner} token via
     * {@link AhRenderRuleBuilder#owner(Object)}. Owners are compared with
     * {@link Object#equals(Object)}.
     * @since 0.13.0
     */
    static void unregisterAll(@Nullable Object owner) {
        AhRenderRuleRegistryImpl.unregisterAll(owner);
    }
}
