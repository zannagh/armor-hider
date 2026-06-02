package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

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