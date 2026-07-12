package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that allows the client to set whether the opacity of the hat or skull should be affected by the opacity of the armor.<br/><br/>
 *
 * Note: This does not affect hats any longer.
 *
 * @since 0.5.0
 */
public class OpacityAffectingHatOrSkullItem extends BooleanConfigItem {
    public OpacityAffectingHatOrSkullItem(boolean currentValue) {
        super(currentValue);
    }

    public OpacityAffectingHatOrSkullItem() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
