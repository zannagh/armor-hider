package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that allows the client to set whether the opacity of the elytra should be affected by the opacity of the armor.
 *
 * @since 0.5.0
 */
public class OpacityAffectingElytraItem extends BooleanConfigItem {
    public OpacityAffectingElytraItem(boolean currentValue) {
        super(currentValue);
    }

    public OpacityAffectingElytraItem() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
