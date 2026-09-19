package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.UUIDConfigItem;

import java.util.UUID;

public class PlayerUuid extends UUIDConfigItem {

    /**
     * The sentinel "no player" id. Deliberately a constant rather than a fresh {@link UUID#randomUUID()}:
     * this method is reached from {@code new PlayerConfig()}, which {@code SlotModification.empty()} used to
     * build per model part and per baked quad on the render hot path - and {@code randomUUID()} is a
     * synchronized SecureRandom draw. It is the same all-zero id
     * {@code ArmorHiderPlayerConfigApi.DEFAULT_PLAYER_ID} already uses for the unknown/global-override
     * player, so nothing downstream sees a new shape of value, and it round-trips through the config
     * item (de)serializer unchanged.
     */
    private static final UUID UNASSIGNED = new UUID(0L, 0L);

    public PlayerUuid(UUID uuid) {
        super(uuid);
    }

    public PlayerUuid() {
        super();
    }

    @Override
    public UUID getDefaultValue() {
        return UNASSIGNED;
    }
}
