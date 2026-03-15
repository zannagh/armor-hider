package de.zannagh.armorhider.configuration.abstractions;

public abstract class StringConfigItem extends ConfigurationItemBase<String> {

    public StringConfigItem(String currentValue) {
        super(currentValue);
    }

    public StringConfigItem() {
        super();
    }
}
