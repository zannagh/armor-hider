package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that allows the client to set whether the Elytra should be rendered in flight (when true) or
 * should be intercepted as usual by the mod (when false).
 *
 * @since 0.12.14
 */
public class ElytraInFlight extends BooleanConfigItem {
    public ElytraInFlight(boolean currentValue) {
        super(currentValue);
    }

    public ElytraInFlight() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}
