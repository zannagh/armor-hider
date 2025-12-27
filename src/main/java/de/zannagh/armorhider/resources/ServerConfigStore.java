package de.zannagh.armorhider.resources;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import de.zannagh.armorhider.ArmorHider;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ServerConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LEGACY_MAP_TYPE = new TypeToken<Map<UUID, PlayerConfig>>(){}.getType();

    private final Path file;
    private ServerConfiguration configuration = new ServerConfiguration();

    private ServerConfigStore(Path file) { this.file = file; }

    public static ServerConfigStore open() {
        var file = new File("config", "armor-hider-server.json").toPath();
        ServerConfigStore store = new ServerConfigStore(file);
        store.load();
        return store;
    }

    public ServerConfiguration getConfig() { return configuration; }

    public void setServerCombatDetection(Boolean enabled) {
        configuration.enableCombatDetection = enabled;
    }

    private void load() {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    JsonElement element = JsonParser.parseReader(r);

                    if (element.isJsonObject()) {
                        JsonObject obj = element.getAsJsonObject();

                        // Check if this is the new format (has "playerConfigs" field)
                        if (obj.has("playerConfigs")) {
                            // New format - deserialize directly
                            configuration = GSON.fromJson(element, ServerConfiguration.class);
                            ArmorHider.LOGGER.info("Loaded server config (new format).");
                        } else {
                            // Old format - it's a flat map of UUID -> PlayerConfig
                            Map<UUID, PlayerConfig> legacyData = GSON.fromJson(element, LEGACY_MAP_TYPE);
                            if (legacyData != null) {
                                configuration = ServerConfiguration.fromLegacyFormat(legacyData);
                                ArmorHider.LOGGER.info("Migrated server config (legacy format).");
                                save();
                            }
                        }
                    }
                }
                if (configuration.enableCombatDetection == null) {
                    configuration.enableCombatDetection = true;
                    save();
                }
            } else {
                save();
                ArmorHider.LOGGER.info("Setup server config.");
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Server config load failed", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(configuration, ServerConfiguration.class, w);
                ArmorHider.LOGGER.info("Saved server config.");
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Server config save failed", e);
        }
    }

    public void put(UUID uuid, PlayerConfig cfg) {
        configuration.playerConfigs.put(uuid, cfg);
        Map<UUID, PlayerConfig> overwrites = new HashMap<>();
        configuration.playerConfigs.forEach((e, k) -> {
            if (k.playerName.equals(cfg.playerName)){
                overwrites.put(e, k);
            }
        });
        configuration.playerNameConfigs.put(cfg.playerName, cfg);
        overwrites.forEach((e, k) -> configuration.playerConfigs.replace(e, k));
        save();
    }
}