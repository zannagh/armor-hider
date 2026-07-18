package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that, when enabled, hides accessory items rendered on a
 * player's body by accessory mods (e.g. Artifacts via Curios / Trinkets / Accessories).
 * <p>
 * This is a plain visibility toggle — accessories are not tied to a vanilla armor slot, so they have
 * no opacity slider. Elytra-like accessories are deliberately NOT governed by this flag: an elytra in
 * any slot continues to follow the chest slider together with {@link OpacityAffectingElytraItem}.
 *
 * @since 0.12.0
 */
public class HideAccessories extends BooleanConfigItem {
    public HideAccessories(boolean currentValue) {
        super(currentValue);
    }

    public HideAccessories() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
