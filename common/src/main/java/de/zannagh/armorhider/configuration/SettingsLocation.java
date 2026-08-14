package de.zannagh.armorhider.configuration;

public enum SettingsLocation {
    /**
     * Armor Hider settings are to be displayed as a button (to go to the settings screen) embedded in the skin customization screen.
     * The displayed screen is the skin customization screen.
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
