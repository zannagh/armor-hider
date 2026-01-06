package de.zannagh.armorhider;

import de.zannagh.armorhider.common.ConfigurationProvider;
import de.zannagh.armorhider.resources.PlayerConfig;

import java.util.UUID;

public class StringPlayerConfigProvider implements ConfigurationProvider<PlayerConfig>{
    
    private String configuration;
    public StringPlayerConfigProvider(String configuration) {
        this.configuration = configuration;
    }
    PlayerConfig current;
    @Override
    public PlayerConfig load() {
        if (configuration.isEmpty()) {
            current = PlayerConfig.empty();
            return current;
        }
        current = PlayerConfig.deserialize(configuration);
        return current;
    }

    @Override
    public void save(PlayerConfig currentValue) {
        configuration = ArmorHider.GSON.toJson(currentValue);
    }

    @Override
    public PlayerConfig getDefault() {
        return PlayerConfig.defaults(UUID.randomUUID(), "dummy");
    }
}
