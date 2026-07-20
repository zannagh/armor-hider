package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * Whether legs-type accessories (belt curios/trinkets) are hidden together with the leggings slot.
 * Only takes effect while {@link AffectAccessories} is enabled.
 *
 * @since 0.12.x
 */
public class AffectLegsAccessory extends BooleanConfigItem {
    public AffectLegsAccessory(boolean currentValue) {
        super(currentValue);
    }

    public AffectLegsAccessory() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}
