package de.zannagh.armorhider.client.api;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import java.util.function.Predicate;

/**
 * Terminal stage of {@link AhRenderRuleBuilder}: attach the condition and the rule is registered.
 *
 * @since 0.13.0
 */
@ApiStatus.NonExtendable
@ApiStatus.Experimental
public interface AhRenderRuleCondition {

    /**
     * Also suppresses the glint, on top of the opacity effect already chosen. No-op if the effect
     * was already {@link AhRenderRuleBuilder#disableGlint()}.
     *
     * @return this stage.
     * @since 0.13.0
     */
    @NonNull AhRenderRuleCondition andDisableGlint();

    /**
     * Registers the rule with a condition on the rendered player. The rule does <b>not</b> match
     * while the entity cannot be resolved on the client (out of render distance, already removed) -
     * use {@link #whenMatching(Predicate)} if the player name alone is enough.
     *
     * @return the handle to unregister this rule with.
     * @since 0.13.0
     */
    @NonNull AhRenderRule when(@NonNull Predicate<Player> condition);

    /**
     * Registers the rule with a condition on the full {@link AhHideContext}.
     *
     * @return the handle to unregister this rule with.
     * @since 0.13.0
     */
    @NonNull AhRenderRule whenMatching(@NonNull Predicate<AhHideContext> condition);
}
