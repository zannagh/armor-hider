package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * Whether the client-side global override configuration is applied to EVERY other player (overriding their
 * own server-broadcast settings), rather than only to unknown/non-mod players. Individual per-player
 * overrides still win, and the server-wide force-off still takes precedence. Server-independent.
 *
 * @since 0.12.0-pre.10
 */
public class UseGlobalOverrideForAllPlayers extends BooleanConfigItem {

    public UseGlobalOverrideForAllPlayers(boolean currentValue) {
        super(currentValue);
    }

    public UseGlobalOverrideForAllPlayers() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
