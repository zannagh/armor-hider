package de.zannagh.armorhider.common.configuration.items;

import de.zannagh.armorhider.common.configuration.ConfigurationItemBase;

import java.util.UUID;

public abstract class UUIDConfigItem extends ConfigurationItemBase<UUID> {

    public UUIDConfigItem(UUID defaultValue) {
        super(defaultValue);
    }

    public UUIDConfigItem() {
        super();
    }
}
