package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * Whether chest-type accessories (necklace curios/trinkets) are hidden together with the chest slot.
 * Only takes effect while {@link AffectAccessories} is enabled.
 *
 * @since 0.12.x
 */
public class AffectChestAccessory extends BooleanConfigItem {
    public AffectChestAccessory(boolean currentValue) {
        super(currentValue);
    }

    public AffectChestAccessory() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}
