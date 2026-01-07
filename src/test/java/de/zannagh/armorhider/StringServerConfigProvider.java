package de.zannagh.armorhider;

import de.zannagh.armorhider.common.ConfigurationProvider;
import de.zannagh.armorhider.resources.ServerConfiguration;

import java.io.IOException;

public class StringServerConfigProvider implements ConfigurationProvider<ServerConfiguration> {
    
    private String configuration;
    
    private ServerConfiguration current;
    
    public StringServerConfigProvider(String configuration){
        this.configuration = configuration;
        current = load();
    }
    
    @Override
    public ServerConfiguration load() {
        if (configuration.isEmpty()) {
            current = new ServerConfiguration();
            return current;
        }
        try {
            current = ServerConfiguration.deserialize(configuration);
        } catch (IOException e) {
            current = new ServerConfiguration();
        }
        return current;
    }

    @Override
    public void save(ServerConfiguration currentValue) {
        configuration = ArmorHider.GSON.toJson(currentValue);
    }

    @Override
    public void saveCurrent() {
        configuration = ArmorHider.GSON.toJson(current);
    }

    @Override
    public void setValue(ServerConfiguration newValue) {
        current = newValue;
    }

    @Override
    public ServerConfiguration getValue() {
        return current;
    }

    @Override
    public ServerConfiguration getDefault() {
        return new ServerConfiguration();
    }
}
