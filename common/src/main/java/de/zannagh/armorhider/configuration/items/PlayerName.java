package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.StringConfigItem;

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
