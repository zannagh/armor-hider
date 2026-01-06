package de.zannagh.armorhider.resources;

import com.google.gson.reflect.TypeToken;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.common.ConfigurationProvider;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerConfigFileProvider implements ConfigurationProvider<ServerConfiguration> {
    private final Path file;
    private ServerConfiguration CURRENT = new ServerConfiguration();
    public ServerConfigFileProvider(Path file){
        this.file = file;
        CURRENT = load();
    }

    @Override
    public ServerConfiguration load() {
        ServerConfiguration configuration = new ServerConfiguration();
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    configuration = ServerConfiguration.deserialize(r);
                }
            } else {
                save(configuration);
                ArmorHider.LOGGER.info("Setup new server config due to missing file.");
            }
        } catch (IOException e) {
            ArmorHider.LOGGER.error("Server config load failed", e);
            configuration.setHasChangedFromSerializedContent();
        }
        if (configuration.hasChangedFromSerializedContent()) {
            save(configuration);
        }
        return configuration;
    }

    @Override
    public void save(ServerConfiguration currentValue) {
        setValue(currentValue);
        saveCurrent();
    }

    @Override
    public void saveCurrent() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                ArmorHider.GSON.toJson(CURRENT, ServerConfiguration.class, w);
                ArmorHider.LOGGER.info("Saved server config.");
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Server config save failed", e);
        }
    }

    @Override
    public void setValue(ServerConfiguration newValue) {
        CURRENT = newValue;
    }

    @Override
    public ServerConfiguration getValue() {
        return CURRENT;
    }

    @Override
    public ServerConfiguration getDefault() {
        return new ServerConfiguration();
    }
}
