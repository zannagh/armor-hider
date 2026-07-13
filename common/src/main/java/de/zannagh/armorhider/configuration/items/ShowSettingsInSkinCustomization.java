package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * A client-side {@link BooleanConfigItem} that allows the client to set whether the settings should be shown in the skin customization menu or on a dedicated screen.
 *
 * @since 0.10.4-pre.1
 */
public class ShowSettingsInSkinCustomization extends BooleanConfigItem {
    public ShowSettingsInSkinCustomization(boolean currentValue) {
        super(currentValue);
    }

    public ShowSettingsInSkinCustomization() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
