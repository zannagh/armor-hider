package de.zannagh.armorhider.configuration;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.packets.PlayerConfig;

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
                    if (current == null) {
                        throw new IllegalStateException("Client config file was empty or deserialized to null.");
                    }

                    if (current.configVersion < PlayerConfig.CURRENT_CONFIG_VERSION) {
                        current = migrateConfig(current);
                    }

                    if (current.hasChangedFromSerializedContent()) {
                        save(current);
                    }
                    ArmorHider.LOGGER.info("Loaded client config from file.");
                    return current;
                }
            } else {
                var defaults = getDefault();
                save(defaults);
                return defaults;
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to load client config, replacing with defaults!", e);
            var defaults = getDefault();
            save(defaults);
            return defaults;
        }
    }

    private PlayerConfig migrateConfig(PlayerConfig old) {
        return PlayerConfig.migrate(old);
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
    public void saveCurrent() {
        save(current);
    }

    @Override
    public PlayerConfig getDefault() {
        return PlayerConfig.defaults(UUID.randomUUID(), "dummy");
    }
}
