package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that determines whether armor hider is forced off on other players.
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
