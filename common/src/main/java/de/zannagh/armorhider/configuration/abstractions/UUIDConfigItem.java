package de.zannagh.armorhider.configuration.abstractions;

import java.util.UUID;

public abstract class UUIDConfigItem extends ConfigurationItemBase<UUID> {

    public UUIDConfigItem(UUID defaultValue) {
        super(defaultValue);
    }

    public UUIDConfigItem() {
        super();
    }
}
