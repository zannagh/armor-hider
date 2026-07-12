package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that allows the client to set whether the shield should be shown when blocking.
 *
 * @since 0.11.4-pre.1
 */
public class ShowShieldWhenBlocking extends BooleanConfigItem {
    public ShowShieldWhenBlocking(boolean currentValue) {
        super(currentValue);
    }

    public ShowShieldWhenBlocking() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
