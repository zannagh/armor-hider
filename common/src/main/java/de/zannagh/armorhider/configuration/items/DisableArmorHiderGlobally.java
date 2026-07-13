package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A {@link BooleanConfigItem} that determines whether armor hider is disabled globally. Used both by
 * the server (has the upper hand) and the client (if the server has disabled it).
 *
 * @since 0.6.0
 */
public class DisableArmorHiderGlobally extends BooleanConfigItem {
    public DisableArmorHiderGlobally(boolean currentValue) {
        super(currentValue);
    }

    public DisableArmorHiderGlobally() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
