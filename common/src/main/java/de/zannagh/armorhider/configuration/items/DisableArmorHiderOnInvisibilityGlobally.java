package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A server-side {@link BooleanConfigItem} that determines whether armor hider is disabled on invisibility.
 */
public class DisableArmorHiderOnInvisibilityGlobally extends BooleanConfigItem {

    public DisableArmorHiderOnInvisibilityGlobally(boolean currentValue) {
        super(currentValue);
    }

    public DisableArmorHiderOnInvisibilityGlobally() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
