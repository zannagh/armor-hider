package de.zannagh.armorhider.configuration;

/**
 * How Armor Hider treats a custom EMF (Fresh Animations) player model when the body armor is hidden.
 * <p>
 * Only relevant while Entity Model Features is present. The default, {@link #KEEP}, leaves the custom
 * model alone - with the equipment signal preserved (see {@code EmfCompat#clearEquipment}), well-behaved
 * add-ons like the Fresh Animations Player Extension already pose the arms correctly. The other two
 * modes are an opt-in safety net for models that still show a seam once the armor stops covering it.
 *
 * @since 0.12.x
 */
public enum EmfHiddenModelMode {

    /** Do nothing special - the custom EMF model keeps rendering (default). */
    KEEP,
    /** Render the whole vanilla model while the body is hidden (no custom-model seam, no custom animation). */
    VANILLA,
    /** Render only the torso and arms as vanilla; keep the custom model for the head and legs. */
    VANILLA_SEAMS;

    /**
     * Resolves a stored name to a mode, falling back to {@link #KEEP} for {@code null} or unknown values
     * so a hand-edited or forward-version config can never throw here.
     *
     * @param name the stored enum name
     * @return the matching mode, or {@link #KEEP}
     */
    public static EmfHiddenModelMode fromName(String name) {
        if (name == null) {
            return KEEP;
        }
        for (EmfHiddenModelMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return KEEP;
    }
}
