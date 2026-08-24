package de.zannagh.armorhider.net.packets;

import org.jspecify.annotations.Nullable;

/**
 * What a player's <em>shared</em> render rules currently resolve to for one piece of equipment, as
 * broadcast to the other clients on an Armor-Hider server.
 *
 * <p>This is an outcome, not a rule: predicates are arbitrary code and cannot be serialised, so the
 * owning client evaluates its own shared rules against itself and sends the result. The values are
 * <b>absolute</b>, exactly like a locally evaluated rule - the receiving client substitutes the
 * opacity for the one it derived from its own view of that player's config, rather than scaling it.</p>
 *
 * <p>A plain class with public fields and a no-arg constructor, matching every other payload here:
 * these travel as GZIP-compressed JSON through {@code CompressedJsonCodec}, and record support in
 * Gson is not something the 1.20.1 bundle can be relied on for.</p>
 *
 * @since 0.13.0
 */
public final class SharedRuleOverride {

    /** Which piece this applies to. Nullable only in the sense that a malformed payload may omit it. */
    public @Nullable SharedRuleTarget target;

    /** Whether {@link #opacity} carries a value. A glint-only rule leaves the opacity alone. */
    public boolean affectsOpacity;

    /** The opacity to render at, {@code 0} hidden to {@code 1} opaque. Meaningless unless {@link #affectsOpacity}. */
    public double opacity = 1.0;

    /** Whether the enchantment glint has to be suppressed. */
    public boolean disableGlint;

    /** Gson. */
    public SharedRuleOverride() {
    }

    public SharedRuleOverride(SharedRuleTarget target, boolean affectsOpacity, double opacity, boolean disableGlint) {
        this.target = target;
        this.affectsOpacity = affectsOpacity;
        this.opacity = opacity;
        this.disableGlint = disableGlint;
    }

    /**
     * @return whether this entry says anything at all. An entry that neither sets an opacity nor
     *         disables the glint is dropped rather than sent, so a receiver never has to reason about it.
     */
    public boolean isMeaningful() {
        return target != null && (affectsOpacity || disableGlint);
    }

    /**
     * @return a fresh instance with the opacity clamped into {@code [0, 1]}. Applied on receipt: the
     *         value comes off the network and the render path multiplies colours with it.
     *         <p>
     *         <b>Always</b> a new object, never {@code this}, even when nothing needed clamping. The
     *         callers are the two stores, and their whole contract is that they own what they hold: a
     *         {@code SharedRuleOverride} is a mutable public-field carrier deserialised straight out of
     *         an inbound payload, so handing the same instance back would let anything still holding
     *         that payload rewrite stored state and silently break the change detection both stores
     *         rely on. The allocation is one small object per announced piece of equipment, on a path
     *         that only runs when a shared state actually changed.
     */
    public SharedRuleOverride sanitizedCopy() {
        return new SharedRuleOverride(target, affectsOpacity,
                Math.max(0.0, Math.min(1.0, opacity)), disableGlint);
    }

    // Equality drives the send-on-change diff in the client broadcaster: an identical snapshot must
    // never produce a packet, or a rule whose predicate is true every tick would flood the server.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SharedRuleOverride that)) {
            return false;
        }
        return target == that.target
                && affectsOpacity == that.affectsOpacity
                && Double.compare(opacity, that.opacity) == 0
                && disableGlint == that.disableGlint;
    }

    @Override
    public int hashCode() {
        int result = target == null ? 0 : target.hashCode();
        result = 31 * result + Boolean.hashCode(affectsOpacity);
        result = 31 * result + Double.hashCode(opacity);
        result = 31 * result + Boolean.hashCode(disableGlint);
        return result;
    }

    @Override
    public String toString() {
        return "SharedRuleOverride[" + target
                + (affectsOpacity ? ", opacity=" + opacity : "")
                + (disableGlint ? ", noGlint" : "")
                + "]";
    }
}
