package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.client.api.AhHideContext;
import de.zannagh.armorhider.client.api.AhRenderRule;
import de.zannagh.armorhider.client.api.AhRenderRuleCondition;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Immutable snapshot of a fully configured rule, taken the moment an effect is chosen on
 * {@link de.zannagh.armorhider.client.api.AhRenderRuleBuilder}.
 * <p>
 * Immutability is what makes builder reuse safe. Builders are values consumers naturally hold onto
 * and register several rules from:
 * <pre>{@code
 * var shared = ArmorHiderRenderApi.rule(CHEST).owner(MY_MOD);
 * shared.hide().when(a);          // opacity 0
 * shared.disableGlint().when(b);  // glint only - must NOT inherit the opacity 0 above
 * }</pre>
 * If this stage aliased the builder, the second registration would silently carry the first
 * effect's opacity and hide the chestplate. Each effect call therefore produces a fresh snapshot,
 * and {@link #andDisableGlint()} produces another rather than mutating this one.
 */
@ApiStatus.Internal
public final class AhRenderRuleConditionImpl implements AhRenderRuleCondition {

    private final AhRuleTarget target;
    private final int priority;
    private final @Nullable Object owner;
    private final boolean shared;
    private final boolean affectsOpacity;
    private final double opacity;
    private final boolean disableGlint;

    public AhRenderRuleConditionImpl(AhRuleTarget target,
                                     int priority,
                                     @Nullable Object owner,
                                     boolean shared,
                                     boolean affectsOpacity,
                                     double opacity,
                                     boolean disableGlint) {
        this.target = target;
        this.priority = priority;
        this.owner = owner;
        this.shared = shared;
        this.affectsOpacity = affectsOpacity;
        this.opacity = opacity;
        this.disableGlint = disableGlint;
    }

    @Override
    public @NonNull AhRenderRuleCondition andDisableGlint() {
        if (disableGlint) {
            return this;
        }
        return new AhRenderRuleConditionImpl(target, priority, owner, shared, affectsOpacity, opacity, true);
    }

    @Override
    public @NonNull AhRenderRule when(@NonNull Predicate<Player> condition) {
        Objects.requireNonNull(condition, "condition");
        return whenMatching(context -> {
            Player player = context.player();
            return player != null && condition.test(player);
        });
    }

    @Override
    public @NonNull AhRenderRule whenMatching(@NonNull Predicate<AhHideContext> condition) {
        Objects.requireNonNull(condition, "condition");
        return AhRenderRuleRegistryImpl.register(
                target, priority, affectsOpacity, opacity, disableGlint, condition, owner, shared);
    }
}
