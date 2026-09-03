package de.zannagh.armorhider.net.packets;

import de.zannagh.eunomia.common.SemanticVersion;
import de.zannagh.eunomia.configuration.PlayerLinkedConfigurationItemBase;
import de.zannagh.eunomia.configuration.ReplicatedPlayerConfig;

import java.util.UUID;

/**
 * eunomia-native wrapper that lets a per-player {@link PlayerConfig} ride eunomia 0.3.0's replicated keyed
 * store ({@code ReplicatedPlayerConfigStore} server-side, {@code ReplicatedClientStore} client-side, and the
 * external HTTP/WebSocket relay). It carries the config as-is and delegates identity + schema to it, so the
 * store keys every player's settings by their authenticated UUID, relays each update to the other clients and
 * dumps the whole set to newcomers on join - the same behaviour over the Minecraft transport and the relay.
 *
 * <p>This is deliberately a <em>wrapper</em> rather than making {@link PlayerConfig} implement
 * {@link ReplicatedPlayerConfig} directly: {@code PlayerConfig} implements armor-hider's own
 * {@code ConfigurationSource} whose {@code getSchemaVersion()} returns an {@code int}, which clashes on erasure
 * with eunomia's {@link SemanticVersion}-returning one. Wrapping keeps {@code PlayerConfig}'s on-disk/on-wire
 * shape (and its int {@code configVersion}) completely unchanged - existing configs load exactly as before -
 * while exposing the eunomia contract here. The int schema version maps to {@code SemanticVersion(n, 0, 0)},
 * which preserves the {@code <} ordering {@code shouldMigrate} relies on.</p>
 */
public final class AhReplicatedPlayerConfig
        extends PlayerLinkedConfigurationItemBase<AhReplicatedPlayerConfig>
        implements ReplicatedPlayerConfig<AhReplicatedPlayerConfig> {

    /** The carried per-player config (the network-stripped form when sent over the wire). */
    public PlayerConfig config;

    public AhReplicatedPlayerConfig() {
        this.config = new PlayerConfig();
    }

    public AhReplicatedPlayerConfig(UUID playerId, PlayerConfig config) {
        super(playerId);
        this.config = config;
    }

    /** Wraps {@code config} keyed by {@code playerId}, carrying its network-stripped form. */
    public static AhReplicatedPlayerConfig forNetwork(UUID playerId, PlayerConfig config) {
        return new AhReplicatedPlayerConfig(playerId, config.forNetwork());
    }

    /** The carried config, re-stamped with this wrapper's authoritative owner id. */
    public PlayerConfig toPlayerConfig() {
        if (config != null && getPlayerId() != null) {
            config.playerId.setValue(getPlayerId());
        }
        return config;
    }

    @Override
    public AhReplicatedPlayerConfig getValue() {
        return this;
    }

    @Override
    public void setValue(AhReplicatedPlayerConfig newValue) {
        this.config = newValue.config;
        setPlayerId(newValue.getPlayerId());
    }

    @Override
    public AhReplicatedPlayerConfig getDefaultValue() {
        return new AhReplicatedPlayerConfig();
    }

    @Override
    public SemanticVersion getSchemaVersion() {
        return new SemanticVersion(config == null ? 0 : config.getSchemaVersion(), 0, 0, null);
    }

    @Override
    public SemanticVersion getCurrentSchemaVersion() {
        return new SemanticVersion(PlayerConfig.CURRENT_CONFIG_VERSION, 0, 0, null);
    }

    @Override
    public AhReplicatedPlayerConfig migrateFrom(AhReplicatedPlayerConfig old) {
        PlayerConfig source = old.config == null ? new PlayerConfig() : old.config;
        return new AhReplicatedPlayerConfig(old.getPlayerId(), source.ensureSchemaFrom(source));
    }
}
