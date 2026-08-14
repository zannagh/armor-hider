package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.SettingsLocation;
import de.zannagh.armorhider.configuration.abstractions.ConfigurationItemBase;
import de.zannagh.armorhider.net.packets.PlayerConfig;

public class SettingsScreenLocation extends ConfigurationItemBase<SettingsLocation> {

    public SettingsScreenLocation(SettingsLocation location) {
        super(location);
    }

    public SettingsScreenLocation() {
        super(SettingsLocation.OPTIONS_SCREEN);
    }

    @Override
    public SettingsLocation getDefaultValue() {
        return SettingsLocation.OPTIONS_SCREEN;
    }

    public void migrate(PlayerConfig oldConfig) {
        //noinspection deprecation
        boolean wasInSkinCustomization = oldConfig.showSettingsInSkinCustomization.getValue();
        setValue(wasInSkinCustomization ? SettingsLocation.SKIN_CUSTOMIZATION : SettingsLocation.OPTIONS_SCREEN);
    }
}
