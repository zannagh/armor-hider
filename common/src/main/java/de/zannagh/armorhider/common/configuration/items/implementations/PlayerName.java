package de.zannagh.armorhider.common.configuration.items.implementations;

import de.zannagh.armorhider.common.configuration.items.StringConfigItem;

public class PlayerName extends StringConfigItem {

    public PlayerName(String currentValue) {
        super(currentValue);
    }

    public PlayerName() {
        super();
    }

    @Override
    public String getDefaultValue() {
        return "dummy";
    }
}
