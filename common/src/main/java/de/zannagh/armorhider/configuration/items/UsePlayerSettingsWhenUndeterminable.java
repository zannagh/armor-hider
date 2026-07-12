package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that allows the client to set whether the player's settings should be used when the armor's opacity is undeterminable.
 *
 * @since 0.6.0
 */
public class UsePlayerSettingsWhenUndeterminable extends BooleanConfigItem {

    public UsePlayerSettingsWhenUndeterminable(boolean currentValue) {
        super(currentValue);
    }

    public UsePlayerSettingsWhenUndeterminable() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}
