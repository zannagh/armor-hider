package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that determines whether armor hider should use the default armor skin in combat.
 *
 * @since 0.10.18-pre.1
 */
public class InCombatUseDefaultArmorSkin extends BooleanConfigItem {

    public InCombatUseDefaultArmorSkin(boolean currentValue) {
        super(currentValue);
    }

    public InCombatUseDefaultArmorSkin() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
