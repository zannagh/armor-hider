package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.client.api.AhHideContext;
import de.zannagh.armorhider.client.api.AhRenderRule;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Storage and evaluation for {@link AhRenderRule}s. Not part of the public API; reached through
 * the static methods on {@link de.zannagh.armorhider.client.api.ArmorHiderRenderApi}, where the
 * precedence semantics are documented.
 * <p>
 * Registration may happen off-thread during mod init, evaluation always happens on the render
 * thread. Each target holds an {@link AtomicReference} to an <em>immutable</em> list: mutators
 * build a fresh sorted list and swap it in with {@code updateAndGet}, so a reader can never
 * observe a half-inserted or partially-sorted state, and the hot path is a single volatile read
 * with no locking. The target map itself is populated once in {@code <clinit>} and never mutated.
 */
@ApiStatus.Internal
public final class AhRenderRuleRegistryImpl {

    /** Same default as the renderer registry: lower numeric priority is stronger. */
    public static final int DEFAULT_PRIORITY = 1000;

    private static final Comparator<AhRenderRuleImpl> BY_PRIORITY =
            Comparator.comparingInt(AhRenderRuleImpl::priority);

    private static final Map<AhRuleTarget, AtomicReference<List<AhRenderRuleImpl>>> RULES =
            new EnumMap<>(AhRuleTarget.class);

    static {
        for (AhRuleTarget target : AhRuleTarget.values()) {
            RULES.put(target, new AtomicReference<>(List.of()));
        }
    }

    private AhRenderRuleRegistryImpl() {}

    public static AhRenderRule register(AhRuleTarget target,
                                        int priority,
                                        boolean affectsOpacity,
                                        double opacity,
                                        boolean disableGlint,
                                        Predicate<AhHideContext> condition,
                                        @Nullable Object owner) {
        var rule = new AhRenderRuleImpl(target, priority, affectsOpacity, opacity, disableGlint, condition, owner);
        RULES.get(target).updateAndGet(current -> {
            var next = new ArrayList<>(current);
            next.add(rule);
            next.sort(BY_PRIORITY);
            return List.copyOf(next);
        });
        return rule;
    }

    public static void unregister(@Nullable AhRenderRule rule) {
        if (!(rule instanceof AhRenderRuleImpl impl)) {
            return;
        }
        removeMatching(impl.target(), candidate -> candidate == impl);
        impl.markUnregistered();
    }

    public static void unregisterAll(@Nullable Object owner) {
        if (owner == null) {
            return;
        }
        // Marks exactly the rules the swap actually removed. Marking in a separate pass beforehand
        // would leave a window in which a rule registered under the same owner between the two
        // passes gets removed but never marked, so isRegistered() would report true forever.
        for (var target : AhRuleTarget.values()) {
            removeMatching(target, candidate -> owner.equals(candidate.owner()));
        }
    }

    /**
     * Drops every registered rule. Used by the client gametests to isolate cases from each other,
     * and available as a reload hook; Armor Hider itself never registers a rule, so nothing in the
     * mod calls this during normal play.
     */
    public static void clear() {
        for (var target : AhRuleTarget.values()) {
            for (var rule : RULES.get(target).get()) {
                rule.markUnregistered();
            }
            RULES.get(target).set(List.of());
        }
    }

    public static List<AhRenderRule> registeredRules() {
        var all = new ArrayList<AhRenderRule>();
        for (var target : AhRuleTarget.values()) {
            all.addAll(RULES.get(target).get());
        }
        return List.copyOf(all);
    }

    /**
     * Evaluates every rule registered for {@code target}.
     * <p>
     * Precedence is deliberately asymmetric:
     * <ul>
     *   <li><b>Opacity is priority-banded.</b> The strongest priority (lowest numeric value) among
     *       the matching opacity rules decides, and only rules at that priority contribute; among
     *       those, the lowest opacity wins.</li>
     *   <li><b>Glint is not banded.</b> Any matching glint rule suppresses the glint, whatever its
     *       priority. "Disable" is the only direction the API can express, so banding would only let
     *       an unrelated opacity rule silently swallow another mod's glint rule.</li>
     * </ul>
     *
     * @return {@link AhRuleOutcome#unchanged()} when nothing matched - callers keep their own values
     * and no object is allocated.
     */
    public static AhRuleOutcome evaluate(@Nullable AhRuleTarget target,
                                         @Nullable String playerName,
                                         EquipmentSlot slot,
                                         @Nullable ItemStack stack,
                                         boolean isElytra,
                                         PlayerConfig config,
                                         double baseOpacity) {
        if (target == null || playerName == null || playerName.isBlank()) {
            return AhRuleOutcome.unchanged();
        }
        // Zero-consumer fast path: one volatile read plus an EnumMap array index, no allocation.
        var rules = RULES.get(target).get();
        if (rules.isEmpty()) {
            return AhRuleOutcome.unchanged();
        }

        AhHideContextImpl context = null;
        int opacityBand = Integer.MAX_VALUE;
        double opacity = baseOpacity;
        boolean opacitySet = false;
        boolean disableGlint = false;

        for (var rule : rules) {
            if (context == null) {
                context = new AhHideContextImpl(playerName, slot, stack, isElytra, config, baseOpacity);
            }
            if (!matches(rule, context)) {
                continue;
            }
            if (rule.affectsOpacity() && (!opacitySet || rule.priority() <= opacityBand)) {
                // Strictly stronger rule replaces the band outright; an equal-priority peer only
                // contributes if it hides more. Deliberately independent of iteration order.
                //
                // The band is opened on `!opacitySet` rather than on `priority < opacityBand`,
                // because Integer.MAX_VALUE is a legal priority and would otherwise collide with the
                // sentinel: MAX <= MAX passes but MAX < MAX fails, so the first such rule would min
                // against baseOpacity instead of replacing it - quietly breaking the documented
                // guarantee that a rule can also make a piece MORE visible than the config.
                opacity = !opacitySet || rule.priority() < opacityBand
                        ? rule.opacity()
                        : Math.min(opacity, rule.opacity());
                opacityBand = rule.priority();
                opacitySet = true;
            }
            // Glint is a plain OR across ALL matching rules, deliberately NOT priority-banded:
            // "disable" is the only direction the API can express, so banding could only ever let a
            // strong opacity rule suppress a weaker mod's glint rule - surprising, and the two
            // concerns are independent.
            disableGlint |= rule.disableGlint();
        }

        if (!opacitySet && !disableGlint) {
            return AhRuleOutcome.unchanged();
        }
        return new AhRuleOutcome(opacity, disableGlint, true);
    }

    /**
     * Third-party predicates run inside the render loop, so a throwing one must not take the frame
     * down with it. A throw counts as "did not match". The first failure of each rule is logged; the
     * rule is only auto-disabled after {@link AhRenderRuleImpl#MAX_CONSECUTIVE_FAILURES} consecutive
     * throws, so a predicate that trips on a transient condition (the classic being an unguarded
     * {@code ctx.player()} while the entity is out of render distance) recovers instead of dying for
     * the session. Any successful evaluation resets the counter.
     */
    private static boolean matches(AhRenderRuleImpl rule, AhHideContext context) {
        try {
            boolean result = rule.condition().test(context);
            rule.recordSuccess();
            return result;
        } catch (Throwable throwable) {
            if (rule.recordFailureAndShouldDisable(throwable)) {
                unregister(rule);
            }
            return false;
        }
    }

    /**
     * Swaps in a list with every rule matching {@code doomed} removed, then marks exactly those
     * rules unregistered. {@code getAndUpdate} returns the list that was in place immediately before
     * the successful swap, so the marking can never disagree with what was actually removed even if
     * the update function was retried under contention.
     */
    private static void removeMatching(AhRuleTarget target, Predicate<AhRenderRuleImpl> doomed) {
        var before = RULES.get(target).getAndUpdate(current -> {
            var next = new ArrayList<AhRenderRuleImpl>(current.size());
            for (var candidate : current) {
                if (!doomed.test(candidate)) {
                    next.add(candidate);
                }
            }
            return next.size() == current.size() ? current : List.copyOf(next);
        });
        for (var candidate : before) {
            if (doomed.test(candidate)) {
                candidate.markUnregistered();
            }
        }
    }
}
