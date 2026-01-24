package de.zannagh.armorhider.config;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.ConfigurationProvider;
import de.zannagh.armorhider.networking.ClientPacketSender;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ClientConfigManager implements ConfigurationProvider<PlayerConfig> {

    public static final String DEFAULT_PLAYER_NAME = "dummy";

    public static final UUID DEFAULT_PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ConfigurationProvider<PlayerConfig> playerConfigProvider;

    private PlayerConfig CURRENT = PlayerConfig.defaults(DEFAULT_PLAYER_ID, DEFAULT_PLAYER_NAME);

    private ServerConfiguration serverConfiguration = new ServerConfiguration();

    public ClientConfigManager() {
        this.playerConfigProvider = new PlayerConfigFileProvider();
        CURRENT = load();
    }

    public ClientConfigManager(ConfigurationProvider<PlayerConfig> configurationProvider) {
        this.playerConfigProvider = configurationProvider;
        CURRENT = load();
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
        if (ArmorHiderClient.isClientConnectedToServer() && clientNetwork != null) {
            ArmorHider.LOGGER.info("Sending to server...");
            ClientPacketSender.sendToServer(config);
            ArmorHider.LOGGER.info("Send client config package to server.");
        }
    }

    public void saveCurrent() {
        save(CURRENT);
    }

    public PlayerConfig getDefault() {
        return PlayerConfig.empty();
    }

    public void setAndSendServerConfig(boolean combatDetection, boolean forceArmorHiderOff) {
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            return;
        }
        serverConfiguration.serverWideSettings.enableCombatDetection.setValue(combatDetection);
        serverConfiguration.serverWideSettings.forceArmorHiderOff.setValue(forceArmorHiderOff);
        setAndSendServerWideSettings(serverConfiguration.serverWideSettings);
    }

    public void setAndSendServerCombatDetection(boolean combatDetection) {
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            return;
        }
        serverConfiguration.serverWideSettings.enableCombatDetection.setValue(combatDetection);
        setAndSendServerWideSettings(serverConfiguration.serverWideSettings);
    }

    public void setAndSendServerWideSettings(ServerWideSettings serverWideSettings) {
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            ArmorHider.LOGGER.info("Player is no admin, suppressing update...");
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

    public ServerConfiguration getServerConfig() {
        return serverConfiguration;
    }

    public void setServerConfig(ServerConfiguration serverConfig) {
        ArmorHider.LOGGER.info("Setting server config...");
        if (serverConfig.serverWideSettings == null) {
            ArmorHider.LOGGER.warn("Received ServerConfiguration with null serverWideSettings, initializing with defaults");
            serverConfig.serverWideSettings = new ServerWideSettings();
        }

        serverConfiguration = serverConfig;
    }

    public PlayerConfig getConfigForPlayer(@NotNull String playerName) {
        if (playerName.equals(ArmorHiderClient.getCurrentPlayerName())) {
            return CURRENT;
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
}