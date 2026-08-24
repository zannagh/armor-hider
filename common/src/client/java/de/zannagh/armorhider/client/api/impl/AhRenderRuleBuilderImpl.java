package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.client.api.AhRenderRuleBuilder;
import de.zannagh.armorhider.client.api.AhRenderRuleCondition;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Mutable first stage of {@link AhRenderRuleBuilder}: target, priority, owner and the shared flag.
 * <p>
 * Choosing an effect hands off to an immutable {@link AhRenderRuleConditionImpl} snapshot rather
 * than returning {@code this}, so a builder can be safely reused for several registrations without
 * one effect bleeding into the next.
 */
@ApiStatus.Internal
public final class AhRenderRuleBuilderImpl implements AhRenderRuleBuilder {

    private final AhRuleTarget target;
    private int priority = AhRenderRuleRegistryImpl.DEFAULT_PRIORITY;
    private @Nullable Object owner;
    private boolean shared;

    public AhRenderRuleBuilderImpl(AhRuleTarget target) {
        this.target = target;
    }

    @Override
    public @NonNull AhRenderRuleBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public @NonNull AhRenderRuleBuilder owner(@Nullable Object owner) {
        this.owner = owner;
        return this;
    }

    @Override
    public @NonNull AhRenderRuleBuilder shared() {
        this.shared = true;
        return this;
    }

    @Override
    public @NonNull AhRenderRuleCondition hide() {
        return opacity(0f);
    }

    @Override
    public @NonNull AhRenderRuleCondition opacity(float opacity) {
        double clamped = Math.max(0.0, Math.min(1.0, (double) opacity));
        return new AhRenderRuleConditionImpl(target, priority, owner, shared, true, clamped, false);
    }

    @Override
    public @NonNull AhRenderRuleCondition disableGlint() {
        return new AhRenderRuleConditionImpl(target, priority, owner, shared, false, 1.0, true);
    }
}
