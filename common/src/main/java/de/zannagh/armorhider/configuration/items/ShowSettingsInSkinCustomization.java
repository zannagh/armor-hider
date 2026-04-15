package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

public class ShowSettingsInSkinCustomization extends BooleanConfigItem {
    public ShowSettingsInSkinCustomization(boolean currentValue) {
        super(currentValue);
    }

    public ShowSettingsInSkinCustomization() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
