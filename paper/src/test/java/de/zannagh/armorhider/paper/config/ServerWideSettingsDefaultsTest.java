package de.zannagh.armorhider.paper.config;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Schema shape and forward-migration for the server-wide settings block. This is the server's only
 * source of the four admin toggles, so its defaults and its "fill missing on upgrade" behaviour are
 * load-bearing.
 */
@DisplayName("ServerWideSettingsDefaults schema and migration")
class ServerWideSettingsDefaultsTest {

    @Test
    @DisplayName("create() populates every field with the shipped defaults")
    void createHasAllDefaults() {
        JsonObject settings = ServerWideSettingsDefaults.create();
        assertEquals(ServerWideSettingsDefaults.CURRENT_CONFIG_VERSION,
                settings.get(ServerWideSettingsDefaults.CONFIG_VERSION).getAsInt());
        assertTrue(settings.get(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION).getAsBoolean());
        assertFalse(settings.get(ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF).getAsBoolean());
        assertFalse(settings.get(ServerWideSettingsDefaults.DISABLE_ON_INVISIBILITY).getAsBoolean());
        assertTrue(settings.get(ServerWideSettingsDefaults.ALLOW_INDIVIDUAL_CONFIGURATIONS).getAsBoolean());
        assertEquals(5, settings.size(), "exactly the five documented keys");
    }

    @Test
    @DisplayName("fillMissing(null) returns a full default block")
    void fillMissingNullReturnsDefaults() {
        JsonObject filled = ServerWideSettingsDefaults.fillMissing(null);
        assertEquals(ServerWideSettingsDefaults.create(), filled);
    }

    @Test
    @DisplayName("fillMissing adds only absent keys and preserves present values")
    void fillMissingPreservesPresentValues() {
        JsonObject partial = new JsonObject();
        // A non-default value that must survive the upgrade untouched.
        partial.addProperty(ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF, true);

        JsonObject filled = ServerWideSettingsDefaults.fillMissing(partial);

        assertTrue(filled.get(ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF).getAsBoolean(),
                "present value must be preserved, not reset to default");
        assertTrue(filled.has(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION));
        assertTrue(filled.has(ServerWideSettingsDefaults.DISABLE_ON_INVISIBILITY));
        assertTrue(filled.has(ServerWideSettingsDefaults.ALLOW_INDIVIDUAL_CONFIGURATIONS));
    }

    @Test
    @DisplayName("fillMissing replaces an explicit JSON null with the default")
    void fillMissingReplacesJsonNull() {
        JsonObject withNull = ServerWideSettingsDefaults.create();
        withNull.add(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION, com.google.gson.JsonNull.INSTANCE);

        JsonObject filled = ServerWideSettingsDefaults.fillMissing(withNull);

        assertTrue(filled.get(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION).getAsBoolean());
    }

    @Test
    @DisplayName("fillMissing always bumps configVersion to current")
    void fillMissingBumpsConfigVersion() {
        JsonObject stale = new JsonObject();
        stale.addProperty(ServerWideSettingsDefaults.CONFIG_VERSION, 1);
        JsonObject filled = ServerWideSettingsDefaults.fillMissing(stale);
        assertEquals(ServerWideSettingsDefaults.CURRENT_CONFIG_VERSION,
                filled.get(ServerWideSettingsDefaults.CONFIG_VERSION).getAsInt());
    }

    @Test
    @DisplayName("readBoolean falls back to the shipped default on absent, null, or non-primitive")
    void readBooleanFallsBack() {
        // enableCombatDetection defaults to true, forceArmorHiderOff to false.
        assertTrue(ServerWideSettingsDefaults.readBoolean(null, ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION));
        assertFalse(ServerWideSettingsDefaults.readBoolean(null, ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF));

        JsonObject empty = new JsonObject();
        assertTrue(ServerWideSettingsDefaults.readBoolean(empty, ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION));

        JsonObject malformed = new JsonObject();
        malformed.add(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION, new JsonObject()); // non-primitive
        assertTrue(ServerWideSettingsDefaults.readBoolean(malformed, ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION));
    }

    @Test
    @DisplayName("readBoolean returns a present, well-formed value")
    void readBooleanReadsPresentValue() {
        JsonObject settings = new JsonObject();
        settings.addProperty(ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF, true);
        assertTrue(ServerWideSettingsDefaults.readBoolean(settings, ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF));
    }
}
