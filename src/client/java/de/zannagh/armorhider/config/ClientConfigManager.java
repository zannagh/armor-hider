package de.zannagh.armorhider.config;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.ConfigurationProvider;
import de.zannagh.armorhider.netPackets.AdminSettingsC2SPacket;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.netPackets.SettingsC2SPacket;
import de.zannagh.armorhider.resources.ServerConfiguration;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.*;

public class ClientConfigManager {
    
    private final ConfigurationProvider<PlayerConfig> playerConfigProvider;

    private PlayerConfig CURRENT = PlayerConfig.defaults(UUID.randomUUID(), "dummy");
    
    private ServerConfiguration serverConfiguration = new ServerConfiguration();
    
    public ClientConfigManager() {
        this.playerConfigProvider = new PlayerConfigFileProvider();
    }
    
    public void updateName(String name) {
        CURRENT.playerName.setValue(name);
        save();
    }

    public void updateId(UUID id) {
        CURRENT.playerId.setValue(id);
        save();
    }
    
    public void load() {
        CURRENT = playerConfigProvider.load();
    }

    public void save() {
        playerConfigProvider.save(CURRENT);
        if (ArmorHiderClient.isClientConnectedToServer()) {
            ArmorHider.LOGGER.info("Sending to server...");
            ClientPlayNetworking.send(new SettingsC2SPacket(get()));
            ArmorHider.LOGGER.info("Send client config package to server.");
        }
    }
    
    public void setAndSendServerCombatDetection(boolean enabled){
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            return;
        }
        serverConfiguration.enableCombatDetection = enabled;
        setAndSendServerConfig(serverConfiguration);
    }
    
    public void setAndSendServerConfig(ServerConfiguration serverConfig) {
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            ArmorHider.LOGGER.info("Player is no admin, suppressing update...");
            return;
        }
        serverConfiguration = serverConfig;
        ArmorHider.LOGGER.info("Sending server config to server...");
        ClientPlayNetworking.send(new AdminSettingsC2SPacket(serverConfig.enableCombatDetection));
    }
    
    public void setServerConfig(ServerConfiguration serverConfig) {
        ArmorHider.LOGGER.info("Setting server config...");
        serverConfiguration = serverConfig;
    }

    public PlayerConfig get() { return CURRENT; }
    
    public ServerConfiguration getServerConfig() { return serverConfiguration; }
    
    public void set(PlayerConfig cfg) { CURRENT = cfg; save(); }

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
                
                serverConfiguration.putOnRuntime(playerName, playerId, CURRENT);
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