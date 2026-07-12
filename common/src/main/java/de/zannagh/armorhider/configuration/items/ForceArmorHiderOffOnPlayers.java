package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A server-wide {@link BooleanConfigItem} (stored in {@code ServerWideSettings.forceArmorHiderOff}) that,
 * when enabled, forces Armor Hider off for all players on the server, overriding client settings.
 */
public class ForceArmorHiderOffOnPlayers extends BooleanConfigItem {

    public ForceArmorHiderOffOnPlayers(boolean currentValue) {
        super(currentValue);
    }

    public ForceArmorHiderOffOnPlayers() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
