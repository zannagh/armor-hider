package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A {@link BooleanConfigItem} that determines whether combat detection is enabled.
 *
 * @since 0.1.0
 */
public class CombatDetection extends BooleanConfigItem {

    public CombatDetection(boolean currentValue) {
        super(currentValue);
    }

    public CombatDetection() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}
