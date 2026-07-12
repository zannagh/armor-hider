package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderPlayerConfigApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.net.ClientPacketSender;
import de.zannagh.armorhider.client.utils.McClientUtils;
import de.zannagh.armorhider.configuration.ConfigurationProvider;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.server.ServerConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class AhPlayerConfigApiImpl implements ArmorHiderPlayerConfigApi, ConfigurationProvider<PlayerConfig> {

    private final ConfigurationProvider<PlayerConfig> playerConfigProvider;

    private PlayerConfig CURRENT = PlayerConfig.defaults(DEFAULT_PLAYER_ID, DEFAULT_PLAYER_NAME);

    private @Nullable ServerConfiguration serverConfiguration;

    private final HashMap<UUID, Consumer<@Nullable String>> configListeners = new HashMap<>();

    public AhPlayerConfigApiImpl() {
        this.playerConfigProvider = new de.zannagh.armorhider.configuration.PlayerConfigFileProvider();
        CURRENT = load();
    }

    public AhPlayerConfigApiImpl(ConfigurationProvider<PlayerConfig> configurationProvider) {
        this.playerConfigProvider = configurationProvider;
        CURRENT = load();
    }

    @Override
    public UUID addConfigChangeListener(Consumer<@Nullable String> listener) {
        var uuid = UUID.randomUUID();
        configListeners.put(uuid, listener);
        return uuid;
    }

    @Override
    public void removeConfigChangeListener(UUID listenerGuid) {
        configListeners.remove(listenerGuid);
    }

    @Override
    public void notifyConfigListeners(@Nullable String playerName) {
        configListeners.forEach((uuid, listener) -> listener.accept(playerName));
    }

    public PlayerConfig load() {
        return playerConfigProvider.getValue();
    }

    public PlayerConfig getDefault() {
        return ArmorHiderPlayerConfigApi.getDefault();
    }

    public void save(PlayerConfig config) {
        playerConfigProvider.save(config);
        ClientPacketListener clientNetwork = Minecraft.getInstance().getConnection();
        if (serverConfiguration != null && McClientUtils.isClientConnectedToServer() && clientNetwork != null) {
            ArmorHider.LOGGER.info("Sending to server...");
            // Strip the client-only per-player override map before transmitting (privacy + irrelevant to peers).
            ClientPacketSender.sendToServer(config.forNetwork());
            ArmorHider.LOGGER.info("Send client config package to server.");
        }
        notifyConfigListeners(config.playerName.getValue());
    }

    @Override
    public void saveCurrent() {
        save(CURRENT);
    }

    @Override
    public PlayerConfig getValue() {
        return CURRENT;
    }

    @Override
    public void setValue(PlayerConfig newValue) {
        CURRENT = newValue;
        save(CURRENT);
    }

    @Override
    public void updateLocalPlayerName(String playerName, Optional<Boolean> withSave) {
        CURRENT.playerName.setValue(playerName);
        if (withSave.isPresent() && withSave.get()) {
            save(CURRENT);
        }
    }

    @Override
    public void updateLocalPlayerUuid(UUID id, Optional<Boolean> withSave) {
        CURRENT.playerId.setValue(id);
        if (withSave.isPresent() && withSave.get()) {
            save(CURRENT);
        }
    }

    @Override
    public void saveLocalPlayerConfig(PlayerConfig config) {
        save(config);
    }

    @Override
    public PlayerConfig getLocalPlayerConfig() {
        return CURRENT;
    }

    @Override
    public void setLocalPlayerConfig(PlayerConfig config) {
        CURRENT = config;
        save(CURRENT);
    }

    @Override
    public void setAndSendServerWideSettings(@NonNull ServerWideSettings serverWideSettings) {
        if (ArmorHiderClient.permissionLevel < 3) {
            ArmorHider.LOGGER.info("Player is no admin, suppressing update...");
            return;
        }
        if (serverConfiguration == null) {
            return;
        }
        serverConfiguration.serverWideSettings = serverWideSettings;
        ArmorHider.LOGGER.info("Sending server-wide settings to server...");
        ClientPacketSender.sendToServer(serverWideSettings);
    }

    @Override
    public void setAndSendServerConfig(boolean combatDetection, boolean forceArmorHiderOff, boolean disableArmorHiderOnInvisibilityGlobally, boolean allowIndividualPlayerConfigurations) {
        if (ArmorHiderClient.permissionLevel < 3) {
            return;
        }
        if (serverConfiguration == null) {
            return;
        }
        serverConfiguration.serverWideSettings.enableCombatDetection.setValue(combatDetection);
        serverConfiguration.serverWideSettings.forceArmorHiderOff.setValue(forceArmorHiderOff);
        serverConfiguration.serverWideSettings.disableArmorHiderOnInvisibilityGlobally.setValue(disableArmorHiderOnInvisibilityGlobally);
        serverConfiguration.serverWideSettings.allowIndividualPlayerConfigurations.setValue(allowIndividualPlayerConfigurations);
        setAndSendServerWideSettings(serverConfiguration.serverWideSettings);
    }

    @Override
    public @Nullable ServerConfiguration getServerConfig() {
        return serverConfiguration;
    }

    @Override
    public void setServerConfig(ServerConfiguration serverConfig) {
        ArmorHider.LOGGER.info("Setting server config...");
        serverConfiguration = serverConfig;
        notifyConfigListeners(null);
    }

    @Override
    public void clearServerConfig() {
        ArmorHider.LOGGER.info("Clearing server config...");
        serverConfiguration = null;
        notifyConfigListeners(null);
    }

    @Override
    public @Nullable String getServerKey() {
        var server = Minecraft.getInstance().getCurrentServer();
        if (server == null) {
            return "singleplayer";
        }
        if (server.ip != null && !server.ip.isBlank()) {
            return server.ip;
        }
        return server.name != null ? server.name : "unknown";
    }

    @Override
    public PlayerConfig resolveConfig(@Nullable String playerName) {
        if (playerName != null && playerName.equals(ArmorHiderClient.getCurrentPlayerName())) {
            return CURRENT;
        }

        boolean allowed = areOtherPlayerConfigsAllowed();

        // Null name means the player couldn't be identified — treat as unknown/undeterminable.
        if (playerName == null) {
            return allowed
                    ? resolveUnknownPlayerConfig(DEFAULT_PLAYER_NAME, DEFAULT_PLAYER_ID)
                    : PlayerConfig.defaults(DEFAULT_PLAYER_ID, DEFAULT_PLAYER_NAME);
        }

        // A mod server that disallows client-side other-player config makes all the client controls inert:
        // fall back to the server-transmitted config (if any) or vanilla defaults.
        if (!allowed) {
            var serverConfig = serverConfiguration != null
                    ? serverConfiguration.getPlayerConfigOrDefault(playerName) : null;
            return serverConfig != null ? serverConfig : PlayerConfig.defaults(DEFAULT_PLAYER_ID, playerName);
        }

        // Individual per-player override (requires a mod server that allows it) wins over everything below.
        if (areIndividualConfigsAllowedByServer()) {
            PlayerConfig override = CURRENT.individualConfigurations.getOverride(getServerKey(), playerName);
            if (override != null) {
                return override;
            }
        }

        // Row C: "use the global configuration for all other players" overrides even server-broadcast configs.
        if (CURRENT.useGlobalOverrideForAllPlayers.getValue()) {
            return getGlobalConfigOverride();
        }

        // Server-transmitted config from a modded player, by name then by UUID.
        UUID playerId = DEFAULT_PLAYER_ID;
        if (serverConfiguration != null) {
            var config = serverConfiguration.getPlayerConfigOrDefault(playerName);
            if (config != null) {
                return config;
            }
            var isRemotePlayer = ArmorHiderClient.isPlayerRemotePlayer(playerName);
            if (isRemotePlayer.getA()) {
                //? if >= 1.21.9 {
                playerId = isRemotePlayer.getB().getProfile().id();
                config = serverConfiguration.getPlayerConfigOrDefault(isRemotePlayer.getB().getProfile().id());
                //?}
                //? if < 1.21.9 {
                /*playerId = isRemotePlayer.getB().getProfile().getId();
                config = serverConfiguration.getPlayerConfigOrDefault(isRemotePlayer.getB().getProfile().getId());
                *///?}
                if (config != null) {
                    return config;
                }
            }
        }

        // Unknown player (no server config) — Row B decides the source.
        return resolveUnknownPlayerConfig(playerName, playerId);
    }

    @Override
    public PlayerConfig resolveUnknownPlayerConfig(String playerName, UUID playerId) {
        if (CURRENT.usePlayerSettingsWhenUndeterminable.getValue()) {
            return CURRENT.deepCopy(playerName, playerId);
        }
        return getGlobalConfigOverride();
    }
}
