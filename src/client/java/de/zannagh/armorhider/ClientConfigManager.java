package de.zannagh.armorhider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import de.zannagh.armorhider.net.SettingsC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ClientConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = new File("config", "armor-hider.json").toPath();

    private static PlayerConfig CURRENT = PlayerConfig.defaults(UUID.randomUUID(), "dummy");
    
    private static Map<UUID, PlayerConfig> SERVERCONFIG = new HashMap<>();

    public static void updateName(String name){
        CURRENT.playerName = name;
        save();
    }
    
    public static boolean isEnabled(){
        return CURRENT.helmetTransparency < 1 && CURRENT.chestTransparency < 1 && CURRENT.legsTransparency < 1 && CURRENT.bootsTransparency < 1;
    }
    
    public static void setEnabled(boolean enabled){
        if (enabled) {
            CURRENT.helmetTransparency = 1;
            CURRENT.chestTransparency = 1;
            CURRENT.legsTransparency = 1;
            CURRENT.bootsTransparency = 1;
        }
        else {
            CURRENT.helmetTransparency = 0;
            CURRENT.chestTransparency = 0;
            CURRENT.legsTransparency = 0;
            CURRENT.bootsTransparency = 0;
        }
        save();
    }
    
    public static void setHelmet(boolean enabled){
        CURRENT.helmetTransparency = enabled ? 1.0 : 0.0;
        save();
    }

    public static void setChest(boolean enabled){
        CURRENT.chestTransparency = enabled ? 1.0 : 0.0;
        save();
    }

    public static void setLegs(boolean enabled){
        CURRENT.legsTransparency = enabled ? 1.0 : 0.0;
        save();
    }

    public static void setBoots(boolean enabled){
        CURRENT.bootsTransparency = enabled ? 1.0 : 0.0;
        save();
    }
    
    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    PlayerConfig c = GSON.fromJson(r, PlayerConfig.class);
                    if (c != null) {
                        CURRENT = c;
                        Armorhider.LOGGER.info("Loaded client config from file.");
                        Armorhider.LOGGER.info("Current config: {}", GSON.toJson(CURRENT));
                    }
                }
            } else {
                save();
            }
        } catch (IOException e) {
            Armorhider.LOGGER.error("Failed to load client config!", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(CURRENT, w);
                Armorhider.LOGGER.info("Saved client config to file.");
                if (MinecraftClient.getInstance().isConnectedToLocalServer() || MinecraftClient.getInstance().getServer() != null) {
                    Armorhider.LOGGER.info("Sending to server...");
                    ClientPlayNetworking.send(new SettingsC2SPacket(get()));
                    Armorhider.LOGGER.info("Saved client config and sent to server. New config is {}", GSON.toJson(CURRENT));
                }
            }
        } catch (IOException e) {
            Armorhider.LOGGER.error("Failed to save client config!", e);
        }
    }
    
    public static Map<UUID, PlayerConfig> getServerConfig(){ return SERVERCONFIG; }
    
    public static void setServerConfig(List<PlayerConfig> serverConfig) {
        Armorhider.LOGGER.info("Setting server config to {}", GSON.toJson(serverConfig));
        SERVERCONFIG = new HashMap<>();
        serverConfig.forEach(c -> {
            SERVERCONFIG.put(c.playerId, c);
        });
    }

    public static PlayerConfig get() { return CURRENT; }
    public static void set(PlayerConfig cfg) { CURRENT = cfg; save(); }

    public static PlayerConfig getConfigForPlayer(UUID playerId) {
        if (playerId == null) {
            return CURRENT;
        }
        return SERVERCONFIG.getOrDefault(playerId, CURRENT);
    }

    public static PlayerConfig getConfigForPlayerByName(String playerName) {
        if (playerName == null) {
            return CURRENT;
        }
        return SERVERCONFIG.values().stream().filter(c -> c.playerName.equalsIgnoreCase(playerName)).findFirst().orElse(CURRENT);
    }
}