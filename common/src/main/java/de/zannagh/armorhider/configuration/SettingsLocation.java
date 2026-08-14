package de.zannagh.armorhider.configuration;

public enum SettingsLocation {
    /**
     * The Armor Hider controls are embedded directly into the vanilla Skin Customization screen (as a
     * panel, not a separate button). The keybind opens the Skin Customization screen.
     *
     * @since AH 0.12.14
     */
    SKIN_CUSTOMIZATION,

    /**
     * Armor Hider settings are to be displayed as a button (to go to the settings screen) embedded in the options screen.
     * The displayed screen is a standalone screen.
     *
     * @since AH 0.12.14
     */
    OPTIONS_SCREEN,

    /**
     * Armor Hider settings entry points are to be hidden and only accessible via a keybind.
     * The displayed screen is a standalone screen.
     *
     * @since AH 0.12.14
     */
    HIDDEN
}
