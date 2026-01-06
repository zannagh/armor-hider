package de.zannagh.armorhider.resources;

import de.zannagh.armorhider.common.ConfigurationProvider;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class ServerConfigStore implements ConfigurationProvider<ServerConfiguration> {
    
    private final ConfigurationProvider<ServerConfiguration> configurationProvider;

    public ServerConfigStore(){
        this(new File("config", "armor-hider-server.json").toPath());
    }
    
    public ServerConfigStore(Path file) { 
        this.configurationProvider = new ServerConfigFileProvider(file);
    }
    
    public ServerConfigStore(ConfigurationProvider<ServerConfiguration> configurationProvider){
        this.configurationProvider = configurationProvider;
    }

    public ServerConfiguration getConfig() { return configurationProvider.getValue(); }

    public void setServerCombatDetection(Boolean enabled) {
        configurationProvider.getValue().serverWideSettings.enableCombatDetection.setValue(enabled);
        configurationProvider.saveCurrent();
    }

    @Override
    public ServerConfiguration load() {
        return configurationProvider.load();
    }

    @Override
    public void save(ServerConfiguration currentValue) {
        configurationProvider.save(currentValue);
    }


    @Override
    public void setValue(ServerConfiguration newValue) {
        configurationProvider.setValue(newValue);
    }

    @Override
    public ServerConfiguration getValue() {
        return configurationProvider.getValue();
    }

    @Override
    public ServerConfiguration getDefault() {
        return configurationProvider.getDefault();
    }

    @Override
    public void saveCurrent() {
        configurationProvider.saveCurrent();
    }

    public void put(UUID uuid, PlayerConfig cfg) {
        configurationProvider.getValue().playerConfigs.put(uuid, cfg);
        Map<UUID, PlayerConfig> overwrites = new HashMap<>();
        configurationProvider.getValue().playerConfigs.forEach((e, k) -> {
            if (k.playerName.getValue().equals(cfg.playerName.getValue())){
                overwrites.put(e, k);
            }
        });
        configurationProvider.getValue().playerNameConfigs.put(cfg.playerName.getValue(), cfg);
        overwrites.forEach((e, k) -> configurationProvider.getValue().playerConfigs.replace(e, k));
        configurationProvider.saveCurrent();
    }
}