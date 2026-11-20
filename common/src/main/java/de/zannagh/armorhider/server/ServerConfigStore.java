package de.zannagh.armorhider.server;

import de.zannagh.armorhider.configuration.ConfigurationProvider;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerConfigStore implements ConfigurationProvider<ServerConfiguration> {

    private final ConfigurationProvider<ServerConfiguration> configurationProvider;

    public ServerConfigStore() {
        this(new File("config", "armor-hider-server.json").toPath());
    }

    public ServerConfigStore(Path file) {
        this.configurationProvider = new ServerConfigFileProvider(file);
    }

    public ServerConfigStore(ConfigurationProvider<ServerConfiguration> configurationProvider) {
        this.configurationProvider = configurationProvider;
    }

    public ServerConfiguration getConfig() {
        return configurationProvider.getValue();
    }

    public void setServerCombatDetection(Boolean enabled) {
        configurationProvider.getValue().serverWideSettings.enableCombatDetection.setValue(enabled);
    }

    public void setGlobalOverride(Boolean enabled) {
        configurationProvider.getValue().serverWideSettings.forceArmorHiderOff.setValue(enabled);
    }

    /**
     * Applies all four server-wide toggles from the given settings.
     *
     * @return {@code true} if at least one value differed and was changed, {@code false} if nothing changed.
     */
    public boolean applyServerWideSettings(ServerWideSettings incoming) {
        ServerWideSettings current = configurationProvider.getValue().serverWideSettings;
        boolean changed = current.enableCombatDetection.getValue() != incoming.enableCombatDetection.getValue()
                || current.forceArmorHiderOff.getValue() != incoming.forceArmorHiderOff.getValue()
                || current.disableArmorHiderOnInvisibilityGlobally.getValue() != incoming.disableArmorHiderOnInvisibilityGlobally.getValue()
                || current.allowIndividualPlayerConfigurations.getValue() != incoming.allowIndividualPlayerConfigurations.getValue();
        if (!changed) {
            return false;
        }
        current.enableCombatDetection.setValue(incoming.enableCombatDetection.getValue());
        current.forceArmorHiderOff.setValue(incoming.forceArmorHiderOff.getValue());
        current.disableArmorHiderOnInvisibilityGlobally.setValue(incoming.disableArmorHiderOnInvisibilityGlobally.getValue());
        current.allowIndividualPlayerConfigurations.setValue(incoming.allowIndividualPlayerConfigurations.getValue());
        return true;
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
    public ServerConfiguration getValue() {
        return configurationProvider.getValue();
    }

    @Override
    public void setValue(ServerConfiguration newValue) {
        configurationProvider.setValue(newValue);
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
            if (k.playerName.getValue().equals(cfg.playerName.getValue())) {
                overwrites.put(e, k);
            }
        });
        configurationProvider.getValue().playerNameConfigs.put(cfg.playerName.getValue(), cfg);
        overwrites.forEach((e, k) -> configurationProvider.getValue().playerConfigs.replace(e, k));
        configurationProvider.saveCurrent();
    }
}