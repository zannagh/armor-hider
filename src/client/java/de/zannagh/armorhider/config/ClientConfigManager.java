package de.zannagh.armorhider.config;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.ConfigurationProvider;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.*;

public class ClientConfigManager implements ConfigurationProvider<PlayerConfig> {
    
    private final ConfigurationProvider<PlayerConfig> playerConfigProvider;

    private PlayerConfig CURRENT = PlayerConfig.defaults(UUID.randomUUID(), "dummy");
    
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
    
    public void save(PlayerConfig config){
        playerConfigProvider.save(config);
        if (ArmorHiderClient.isClientConnectedToServer()) {
            ArmorHider.LOGGER.info("Sending to server...");
            ClientPlayNetworking.send(getValue());
            ArmorHider.LOGGER.info("Send client config package to server.");
        }
    }

    public void saveCurrent() {
        save(CURRENT);
    }

    public PlayerConfig getDefault() {
        return PlayerConfig.empty();
    }

    public void setAndSendServerCombatDetection(boolean enabled){
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            return;
        }
        serverConfiguration.serverWideSettings.enableCombatDetection.setValue(enabled);
        setAndSendServerWideSettings(serverConfiguration.serverWideSettings);
    }

    public void setAndSendServerWideSettings(ServerWideSettings serverWideSettings) {
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            ArmorHider.LOGGER.info("Player is no admin, suppressing update...");
            return;
        }
        serverConfiguration.serverWideSettings = serverWideSettings;
        ArmorHider.LOGGER.info("Sending server-wide settings to server...");
        ClientPlayNetworking.send(serverWideSettings);
    }
    
    public void setServerConfig(ServerConfiguration serverConfig) {
        ArmorHider.LOGGER.info("Setting server config...");
        if (serverConfig.serverWideSettings == null) {
            ArmorHider.LOGGER.warn("Received ServerConfiguration with null serverWideSettings, initializing with defaults");
            serverConfig.serverWideSettings = new ServerWideSettings();
        }

        serverConfiguration = serverConfig;
    }

    public PlayerConfig getValue() { return CURRENT; }
    
    public ServerConfiguration getServerConfig() { return serverConfiguration; }
    
    public void setValue(PlayerConfig cfg) { CURRENT = cfg; saveCurrent(); }

    public PlayerConfig getConfigForPlayer(String playerName) {
        if (playerName == null || playerName.equals(ArmorHiderClient.getCurrentPlayerName())) {
            return CURRENT;
        }
        
        var config = serverConfiguration.getPlayerConfigOrDefault(playerName);
        if (config != null) {
            return config;
        }
        else {
            if (!Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getProfile().name().equals(playerName)) {
                UUID playerId = null;
                if (Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getCaseInsensitivePlayerInfo(playerName) instanceof PlayerListEntry entry) {
                    playerId = entry.getProfile().id();
                }
                
                serverConfiguration.put(playerName, playerId, CURRENT);
                return CURRENT;
            }
            ArmorHider.LOGGER.warn("Failed to get config for player by id, trying to retrieve by player name. {} {}", playerName, playerName);
            if (serverConfiguration.getPlayerConfigOrDefault(playerName) != null) {
                return serverConfiguration.getPlayerConfigOrDefault(playerName);
            }
        }
        ArmorHider.LOGGER.warn("Failed to get config for player {}. Returning local settings.", playerName);
        return CURRENT;
    }
}