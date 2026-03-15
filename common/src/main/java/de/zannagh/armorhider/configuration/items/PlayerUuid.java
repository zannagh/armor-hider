package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.UUIDConfigItem;

import java.util.UUID;

public class PlayerUuid extends UUIDConfigItem {

    public PlayerUuid(UUID uuid) {
        super(uuid);
    }

    public PlayerUuid() {
        super();
    }

    @Override
    public UUID getDefaultValue() {
        return UUID.randomUUID();
    }
}
