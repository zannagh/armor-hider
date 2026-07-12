package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.HashMapConfigItem;
import de.zannagh.armorhider.net.packets.PlayerConfig;

import java.util.HashMap;

public class IndividualConfigurations extends HashMapConfigItem<PlayerConfig> {
    public IndividualConfigurations(HashMap<String, PlayerConfig> currentValue) {
        super(currentValue);
    }

    public IndividualConfigurations() {
        super();
    }

    @Override
    public HashMap<String, PlayerConfig> getDefaultValue() {
        return new HashMap<>();
    }

    /** Deep-copies the per-player configs so migrations/copies don't share mutable state. */
    public IndividualConfigurations deepCopy() {
        HashMap<String, PlayerConfig> copy = new HashMap<>();
        for (var entry : getValue().entrySet()) {
            PlayerConfig config = entry.getValue();
            copy.put(entry.getKey(), config.deepCopy(config.playerName.getValue(), config.playerId.getValue()));
        }
        return new IndividualConfigurations(copy);
    }
}
