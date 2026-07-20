package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * Master toggle for accessory hiding (issue #246). When enabled, accessories worn through an
 * accessory provider (Curios / Trinkets / Artifacts) are hidden together with the armor slot their
 * accessory-slot type maps to, refined per region by the {@code affect*Accessory} toggles.
 * <p>
 * Accessories can only be hidden, not faded — providers expose no per-render alpha — so an accessory
 * disappears when its mapped armor slot is fully hidden (opacity at 0), and is otherwise shown.
 *
 * @since 0.12.x
 */
public class AffectAccessories extends BooleanConfigItem {
    public AffectAccessories(boolean currentValue) {
        super(currentValue);
    }

    public AffectAccessories() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
