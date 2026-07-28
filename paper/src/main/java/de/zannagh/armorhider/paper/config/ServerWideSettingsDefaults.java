package de.zannagh.armorhider.paper.config;

import com.google.gson.JsonObject;

/**
 * Defaults and field names for the {@code ServerWideSettings} block, mirroring the mod's
 * {@code ServerWideSettings} config items. These four booleans are the only settings the server
 * ever inspects or mutates.
 */
public final class ServerWideSettingsDefaults {

    /** Schema version of the server-wide settings block ({@code CURRENT_CONFIG_VERSION}). */
    public static final int CURRENT_CONFIG_VERSION = 2;

    public static final String CONFIG_VERSION = "configVersion";
    public static final String ENABLE_COMBAT_DETECTION = "enableCombatDetection";
    public static final String FORCE_ARMOR_HIDER_OFF = "forceArmorHiderOff";
    public static final String DISABLE_ON_INVISIBILITY = "disableArmorHiderOnInvisibilityGlobally";
    public static final String ALLOW_INDIVIDUAL_CONFIGURATIONS = "allowIndividualPlayerConfigurations";

    private ServerWideSettingsDefaults() {
    }

    /** Builds a fresh, fully-populated server-wide settings block. */
    public static JsonObject create() {
        JsonObject settings = new JsonObject();
        settings.addProperty(CONFIG_VERSION, CURRENT_CONFIG_VERSION);
        settings.addProperty(ENABLE_COMBAT_DETECTION, true);
        settings.addProperty(FORCE_ARMOR_HIDER_OFF, false);
        settings.addProperty(DISABLE_ON_INVISIBILITY, false);
        settings.addProperty(ALLOW_INDIVIDUAL_CONFIGURATIONS, true);
        return settings;
    }

    /**
     * Adds any field an older on-disk schema omitted and bumps {@code configVersion}, so a config
     * written by an earlier mod release is brought up to the current shape.
     */
    public static JsonObject fillMissing(JsonObject settings) {
        if (settings == null) {
            return create();
        }
        JsonObject defaults = create();
        for (String key : defaults.keySet()) {
            if (!settings.has(key) || settings.get(key).isJsonNull()) {
                settings.add(key, defaults.get(key));
            }
        }
        settings.addProperty(CONFIG_VERSION, CURRENT_CONFIG_VERSION);
        return settings;
    }

    /** Reads a boolean setting, falling back to the shipped default when absent or malformed. */
    public static boolean readBoolean(JsonObject settings, String key) {
        if (settings == null || !settings.has(key) || !settings.get(key).isJsonPrimitive()) {
            return create().get(key).getAsBoolean();
        }
        return settings.get(key).getAsBoolean();
    }
}
