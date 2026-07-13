package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.HashMapConfigItem;
import de.zannagh.armorhider.net.packets.PlayerConfig;

import java.util.HashMap;

/**
 * A client-side {@link HashMapConfigItem} that allows the client to set individual configurations
 * that will be used instead of the {@link UsePlayerSettingsWhenUndeterminable}'s determination for
 * rendering other players.<br/><br/>
 *
 * The resolution order (see {@code AhPlayerConfigApiImpl.resolveConfig}) is:
 * Server does not force AH off > Client does not disable AH for others > Server allows individual configs >
 * --> an individual config set for the player name wins (it takes precedence over the global override)
 * --> else "use global config for all others" -> use the global override
 * --> else a server-transmitted config for that player -> use that config
 * --> else "apply your settings to unknown players" -> use the viewer's own settings
 * --> else use the global override
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
