package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that determines whether armor hider is disabled on invisibility.
 *
 * @since 0.12.0-pre.5
 */
public class DisableArmorHiderOnInvisibility extends BooleanConfigItem {

    public DisableArmorHiderOnInvisibility(boolean currentValue) {
        super(currentValue);
    }

    public DisableArmorHiderOnInvisibility() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
