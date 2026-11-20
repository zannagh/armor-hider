package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that determines whether the client's settings that have a server-wide
 * counterpart are written into the integrated server's world config while playing singleplayer (not opened to LAN).
 *
 * @since 0.12.17, schema 16
 */
public class SyncServerSettingsInSingleplayer extends BooleanConfigItem {
    public SyncServerSettingsInSingleplayer(boolean currentValue) {
        super(currentValue);
    }

    public SyncServerSettingsInSingleplayer() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}
