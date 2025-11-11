package de.zannagh.armorhider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.zannagh.armorhider.net.SettingsC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = new File("config", "armor-hider.json").toPath();

    private static PlayerConfig CURRENT = PlayerConfig.defaults(UUID.randomUUID());
    
    private static List<PlayerConfig> SERVERCONFIG = new ArrayList<>();

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
                Armorhider.LOGGER.info("Saved client config to file. Sending to server...");
                if (MinecraftClient.getInstance().isRunning()) {
                    ClientPlayNetworking.send(new SettingsC2SPacket(get()));
                    Armorhider.LOGGER.info("Saved client config and sent to server. New config is {}", GSON.toJson(CURRENT));
                }
            }
        } catch (IOException e) {
            Armorhider.LOGGER.error("Failed to save client config!", e);
        }
    }
    
    public static List<PlayerConfig> getServerConfig(){ return SERVERCONFIG; }
    
    public static void setServerConfig(List<PlayerConfig> serverConfig) {
        Armorhider.LOGGER.info("Setting server config to {}", GSON.toJson(serverConfig));
        SERVERCONFIG = serverConfig;
    }

    public static PlayerConfig get() { return CURRENT; }
    public static void set(PlayerConfig cfg) { CURRENT = cfg; save(); }
}