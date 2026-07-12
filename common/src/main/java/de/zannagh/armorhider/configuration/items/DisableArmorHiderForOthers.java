package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A {@link BooleanConfigItem} that determines whether armor hider is disabled for others.
 *
 * @since 0.6.0
 */
public class DisableArmorHiderForOthers extends BooleanConfigItem {

    public DisableArmorHiderForOthers(boolean currentValue) {
        super(currentValue);
    }

    public DisableArmorHiderForOthers() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
