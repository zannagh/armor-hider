package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * Whether head-type accessories (hat curios/trinkets) are hidden together with the helmet slot.
 * Only takes effect while {@link AffectAccessories} is enabled.
 *
 * @since 0.12.x
 */
public class AffectHeadAccessory extends BooleanConfigItem {
    public AffectHeadAccessory(boolean currentValue) {
        super(currentValue);
    }

    public AffectHeadAccessory() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}
