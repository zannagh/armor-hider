package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.ConfigurationProvider;
import de.zannagh.armorhider.client.net.*;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.server.ServerConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ClientConfigManager implements ConfigurationProvider<PlayerConfig> {

    public static final String DEFAULT_PLAYER_NAME = "dummy";

    public static final UUID DEFAULT_PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ConfigurationProvider<PlayerConfig> playerConfigProvider;

    private PlayerConfig CURRENT = PlayerConfig.defaults(DEFAULT_PLAYER_ID, DEFAULT_PLAYER_NAME);

    // The server configuration is null until it has been received from a server running the mod.
    private @Nullable ServerConfiguration serverConfiguration;

    public ClientConfigManager() {
        this.playerConfigProvider = new de.zannagh.armorhider.configuration.PlayerConfigFileProvider();
        CURRENT = load();
    }

    public ClientConfigManager(ConfigurationProvider<PlayerConfig> configurationProvider) {
        this.playerConfigProvider = configurationProvider;
        CURRENT = load();
    }
    
    private final List<Consumer<@Nullable String>> configListeners = new ArrayList<>();

    public void addConfigChangeListener(Consumer<@Nullable String> listener) {
        configListeners.add(listener);
    }

    public void removeConfigChangeListener(Consumer<@Nullable String> listener) {
        configListeners.remove(listener);
    }

    private void notifyConfigListeners(@Nullable String playerName) {
        List.copyOf(configListeners).forEach(l -> l.accept(playerName));
    }

    public void updateName(String name) {
        CURRENT.playerName.setValue(name);
        saveCurrent();
    }

    public void updateId(UUID id) {
        CURRENT.playerId.setValue(id);
        saveCurrent();
    }

    public PlayerConfig load() {
        return playerConfigProvider.getValue();
    }

    public void save(PlayerConfig config) {
        playerConfigProvider.save(config);
        ClientPacketListener clientNetwork = Minecraft.getInstance().getConnection();
        if (serverConfiguration != null && ArmorHiderClient.isClientConnectedToServer() && clientNetwork != null) {
            ArmorHider.LOGGER.info("Sending to server...");
            ClientPacketSender.sendToServer(config);
            ArmorHider.LOGGER.info("Send client config package to server.");
        }
    }

    public void saveCurrent() {
        save(CURRENT);
        notifyConfigListeners(CURRENT.playerName.getValue());
    }

    public PlayerConfig getDefault() {
        return PlayerConfig.empty();
    }

    public void setAndSendServerConfig(boolean combatDetection, boolean forceArmorHiderOff) {
        if (ArmorHiderClient.permissionLevel < 3) {
            return;
        }
        if (serverConfiguration == null) {
            return;
        }
        serverConfiguration.serverWideSettings.enableCombatDetection.setValue(combatDetection);
        serverConfiguration.serverWideSettings.forceArmorHiderOff.setValue(forceArmorHiderOff);
        setAndSendServerWideSettings(serverConfiguration.serverWideSettings);
    }

    public void setAndSendServerCombatDetection(boolean combatDetection) {
        if (ArmorHiderClient.permissionLevel < 3) {
            return;
        }
        if (serverConfiguration == null) {
            return;
        }
        serverConfiguration.serverWideSettings.enableCombatDetection.setValue(combatDetection);
        setAndSendServerWideSettings(serverConfiguration.serverWideSettings);
    }

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

    public PlayerConfig getValue() {
        return CURRENT;
    }

    public void setValue(PlayerConfig cfg) {
        CURRENT = cfg;
        saveCurrent();
    }

    public @Nullable ServerConfiguration getServerConfig() {
        return serverConfiguration;
    }

    public void setServerConfig(ServerConfiguration serverConfig) {
        ArmorHider.LOGGER.info("Setting server config...");
        serverConfiguration = serverConfig;
        notifyConfigListeners(null);
    }

    public PlayerConfig getConfigForPlayer(@Nullable String playerName) {
        if (playerName != null && playerName.equals(ArmorHiderClient.getCurrentPlayerName())) {
            return CURRENT;
        }

        // Null name means the player couldn't be identified (e.g. hidden nametag
        // with no identity hint) — treat as undeterminable
        if (playerName == null) {
            if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().usePlayerSettingsWhenUndeterminable.getValue()) {
                return ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().deepCopy(DEFAULT_PLAYER_NAME, DEFAULT_PLAYER_ID);
            }
            return PlayerConfig.defaults(DEFAULT_PLAYER_ID, DEFAULT_PLAYER_NAME);
        }

        if (serverConfiguration == null) {
            if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().usePlayerSettingsWhenUndeterminable.getValue()) {
                return ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().deepCopy(playerName, DEFAULT_PLAYER_ID);
            }

            return PlayerConfig.defaults(DEFAULT_PLAYER_ID, playerName);
        }

        var config = serverConfiguration.getPlayerConfigOrDefault(playerName);
        if (config != null) {
            return config;
        }

        var isRemotePlayer = ArmorHiderClient.isPlayerRemotePlayer(playerName);

        UUID playerId = DEFAULT_PLAYER_ID;
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

        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().usePlayerSettingsWhenUndeterminable.getValue()) {
            return ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().deepCopy(playerName, playerId);
        }

        return PlayerConfig.defaults(playerId, playerName);
    }
    
    public void markLocalDirty() {
        notifyConfigListeners(ArmorHiderClient.getCurrentPlayerName());
    }
    
    public PlayerConfig local() {
        return CURRENT;
    }

    public boolean isArmorHiderDisabled() {
        if (getValue().disableArmorHider.getValue()) {
            return true;
        }
        ServerConfiguration serverConfig = getServerConfig();
        return serverConfig != null && serverConfig.serverWideSettings.forceArmorHiderOff.getValue();
    }
    
    public boolean shouldApplyCombatDetection(PlayerConfig config) {
        if (config.enableCombatDetection.getValue()) {
            return true;
        }
        var serverConfig = getServerConfig();
        return serverConfig != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();
    }
}
