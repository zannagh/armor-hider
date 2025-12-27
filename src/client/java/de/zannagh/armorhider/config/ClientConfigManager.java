package de.zannagh.armorhider.config;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.netPackets.AdminSettingsC2SPacket;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.netPackets.SettingsC2SPacket;
import de.zannagh.armorhider.resources.ServerConfiguration;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ClientConfigManager {
    
    private static final Path FILE = new File("config", "armor-hider.json").toPath();

    private static PlayerConfig CURRENT = PlayerConfig.defaults(UUID.randomUUID(), "dummy");
    
    private static ServerConfiguration serverConfiguration = new ServerConfiguration();
    public static void updateName(String name) {
        CURRENT.playerName = name;
        save();
    }

    public static void updateId(UUID id) {
        CURRENT.playerId = id;
        save();
    }
    
    public static void setHelmetTransparency(double transparency) {
        CURRENT.helmetTransparency = transparency;
        save();
    }

    public static void setChestTransparency(double transparency) {
        CURRENT.chestTransparency = transparency;
        save();
    }

    public static void setLegsTransparency(double transparency) {
        CURRENT.legsTransparency = transparency;
        save();
    }

    public static void setBootsTransparency(double transparency) {
        CURRENT.bootsTransparency = transparency;
        save();
    }
    
    public static void setCombatDetection(boolean enabled) {
        CURRENT.enableCombatDetection = enabled;
        save();
    }
    
    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    CURRENT = PlayerConfig.Deserialize(r);
                    ArmorHider.LOGGER.info("Loaded client config from file.");
                    ArmorHider.LOGGER.info("Current config: {}", ArmorHider.GSON.toJson(CURRENT));
                }
            } else {
                save();
            }
        } catch (IOException e) {
            ArmorHider.LOGGER.error("Failed to load client config!", e);
            CURRENT = PlayerConfig.defaults(UUID.randomUUID(), "dummy");
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                ArmorHider.GSON.toJson(CURRENT, w);
                ArmorHider.LOGGER.info("Saved client config to file.");
                if (ArmorHiderClient.isClientConnectedToServer()) {
                    ArmorHider.LOGGER.info("Sending to server...");
                    ClientPlayNetworking.send(new SettingsC2SPacket(get()));
                    ArmorHider.LOGGER.info("Send client config package to server.");
                }
            }
        } catch (IOException e) {
            ArmorHider.LOGGER.error("Failed to save client config!", e);
        }
    }
    
    public static void setAndSendServerCombatDetection(boolean enabled){
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            return;
        }
        serverConfiguration.enableCombatDetection = enabled;
        setAndSendServerConfig(serverConfiguration);
    }
    
    public static void setAndSendServerConfig(ServerConfiguration serverConfig) {
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            ArmorHider.LOGGER.info("Player is no admin, suppressing update...");
            return;
        }
        serverConfiguration = serverConfig;
        ArmorHider.LOGGER.info("Sending server config to server...");
        ClientPlayNetworking.send(new AdminSettingsC2SPacket(serverConfig.enableCombatDetection));
    }
    
    public static void setServerConfig(ServerConfiguration serverConfig) {
        ArmorHider.LOGGER.info("Setting server config...");
        serverConfiguration = serverConfig;
    }

    public static PlayerConfig get() { return CURRENT; }
    
    public static ServerConfiguration getServerConfig() { return serverConfiguration; }
    
    public static void set(PlayerConfig cfg) { CURRENT = cfg; save(); }

    public static PlayerConfig getConfigForPlayer(String playerName) {
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