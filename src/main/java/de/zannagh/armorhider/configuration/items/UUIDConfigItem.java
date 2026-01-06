package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.ConfigurationItemBase;

import java.util.UUID;

public abstract class UUIDConfigItem extends ConfigurationItemBase<UUID> {

    public UUIDConfigItem(UUID defaultValue) {
        super(defaultValue);
    }

    public UUIDConfigItem() {
        super();
    }
}
