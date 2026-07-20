package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * Whether feet-type accessories (boot curios/trinkets) are hidden together with the boots slot.
 * Only takes effect while {@link AffectAccessories} is enabled.
 *
 * @since 0.12.x
 */
public class AffectFeetAccessory extends BooleanConfigItem {
    public AffectFeetAccessory(boolean currentValue) {
        super(currentValue);
    }

    public AffectFeetAccessory() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}
