package de.zannagh.armorhider.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private static final Type MAP_TYPE = new TypeToken<Map<UUID, PlayerConfig>>(){}.getType();

    private final Path file;
    private Map<UUID, PlayerConfig> data = new HashMap<>();

    private ServerConfigStore(Path file) { this.file = file; }

    public static ServerConfigStore open() {
        var file = new File("config", "armor-hider-server.json").toPath();
        ServerConfigStore store = new ServerConfigStore(file);
        store.load();
        return store;
    }
    
    public List<PlayerConfig> getConfig() {
        return new ArrayList<>(data.values());
    }

    private void load() {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    Map<UUID, PlayerConfig> m = GSON.fromJson(r, MAP_TYPE);
                    if (m != null) {
                        data = m;
                        ArmorHider.LOGGER.info("Loaded server config.");
                    }
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
                GSON.toJson(data, MAP_TYPE, w);
                ArmorHider.LOGGER.info("Saved server config.");
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Server config save failed", e);
        }
    }
    public void put(UUID uuid, PlayerConfig cfg) {
        data.put(uuid, cfg);
        Map<UUID, PlayerConfig> overwrites = new HashMap<>();
        data.forEach((e, k) -> {
            if (k.playerName.equals(cfg.playerName)){
                overwrites.put(e, k);
            }
        });
        overwrites.forEach((e, k) -> data.replace(e, k));
    }
}