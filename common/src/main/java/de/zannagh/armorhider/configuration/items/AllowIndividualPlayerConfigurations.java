package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

public class AllowIndividualPlayerConfigurations extends BooleanConfigItem {

    public AllowIndividualPlayerConfigurations(boolean currentValue) {
        super(currentValue);
    }

    public AllowIndividualPlayerConfigurations() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}
