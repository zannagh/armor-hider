package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.HashMapConfigItem;
import de.zannagh.armorhider.net.packets.PlayerConfig;

import java.util.HashMap;

/**
 * A client-side {@link HashMapConfigItem} that allows the client to set individual configurations
 * that will be used instead of the {@link UsePlayerSettingsWhenUndeterminable}'s determination for
 * rendering other players.<br/><br/>
 *
 * The logical flow is:
 * Server does not suppress AH > Client does not suppress AH > Server allows individual configs >
 * --> when global override is set -> use global override
 * --> when individual configs contains the player name -> use individual config
 * --> when use player settings for undeterminable -> use player settings
 * --> else use default settings (vanilla)
 *
 * @since 0.12.0-pre.10
 */
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
