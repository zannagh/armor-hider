package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.HashMapConfigItem;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 Represents a configuration item that maps server names with individual configurations.
 The user should be able to configure individual player configurations for each server and player on the server.
 */
public class ServerMappedIndividualConfigurations extends HashMapConfigItem<IndividualConfigurations> {
    public ServerMappedIndividualConfigurations(HashMap<String, IndividualConfigurations> currentValue) {
        super(currentValue);
    }

    public ServerMappedIndividualConfigurations() {
        super();
    }

    @Override
    public HashMap<String, IndividualConfigurations> getDefaultValue() {
        return new HashMap<>();
    }

    /** Returns the stored override for a player on a server, or {@code null} if none exists. */
    public @Nullable PlayerConfig getOverride(@Nullable String serverKey, String playerName) {
        IndividualConfigurations perServer = getValue().get(serverKey);
        return perServer == null ? null : perServer.getValue().get(playerName);
    }

    public boolean hasOverride(@Nullable String serverKey, String playerName) {
        IndividualConfigurations perServer = getValue().get(serverKey);
        return perServer != null && perServer.getValue().containsKey(playerName);
    }

    /** Stores (or replaces) the override for a player on a server, creating the per-server bucket as needed. */
    public void putOverride(@Nullable String serverKey, String playerName, PlayerConfig config) {
        getValue().computeIfAbsent(serverKey, key -> new IndividualConfigurations())
                .getValue().put(playerName, config);
    }

    /** Removes the override for a player on a server, pruning the per-server bucket when it becomes empty. */
    public void removeOverride(@Nullable String serverKey, String playerName) {
        IndividualConfigurations perServer = getValue().get(serverKey);
        if (perServer == null) {
            return;
        }
        perServer.getValue().remove(playerName);
        if (perServer.getValue().isEmpty()) {
            getValue().remove(serverKey);
        }
    }

    /** Deep-copies the whole server -> player -> config map so migrations/copies don't share mutable state. */
    public ServerMappedIndividualConfigurations deepCopy() {
        HashMap<String, IndividualConfigurations> copy = new HashMap<>();
        for (var entry : getValue().entrySet()) {
            copy.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return new ServerMappedIndividualConfigurations(copy);
    }
}
