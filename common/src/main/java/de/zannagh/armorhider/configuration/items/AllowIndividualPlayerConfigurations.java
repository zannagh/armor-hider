package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A {@link BooleanConfigItem} that determines whether individual player configurations are allowed by the server.
 */
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
