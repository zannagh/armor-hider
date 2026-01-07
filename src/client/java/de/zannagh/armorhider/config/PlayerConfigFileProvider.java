package de.zannagh.armorhider.config;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.common.ConfigurationProvider;
import de.zannagh.armorhider.resources.PlayerConfig;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class PlayerConfigFileProvider implements ConfigurationProvider<PlayerConfig> {
    
    private final Path FILE;
    
    private PlayerConfig current;
    
    public PlayerConfigFileProvider() {
        FILE = new File("config", "armor-hider.json").toPath();
        current = load();
    }
    
    @Override
    public PlayerConfig getValue() {
        return current;
    }

    @Override
    public void setValue(PlayerConfig newValue) {
        current = newValue;
    }
    
    @Override
    public PlayerConfig load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    var current = PlayerConfig.deserialize(r);
                    if (current.hasChangedFromSerializedContent()) {
                        save(current);
                    }
                    ArmorHider.LOGGER.info("Loaded client config from file.");
                    ArmorHider.LOGGER.info("Current config: {}", ArmorHider.GSON.toJson(current));
                    return current;
                }
            } else {
                var defaults = getDefault();
                save(defaults);
                return defaults;
            }
        } catch (IOException e) {
            ArmorHider.LOGGER.error("Failed to load client config!", e);
            return getDefault();
        }
    }

    @Override
    public void save(PlayerConfig currentValue) {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                ArmorHider.GSON.toJson(currentValue, writer);
                ArmorHider.LOGGER.info("Saved client config to file.");
            }
        } catch (IOException e) {
            ArmorHider.LOGGER.error("Failed to save client config!", e);
        }
    }
    
    @Override
    public void saveCurrent(){
        save(current);
    }

    @Override
    public PlayerConfig getDefault() {
        return PlayerConfig.defaults(UUID.randomUUID(), "dummy");
    }
}
