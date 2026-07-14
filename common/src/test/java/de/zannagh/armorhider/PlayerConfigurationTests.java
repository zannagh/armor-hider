package de.zannagh.armorhider;

import de.zannagh.armorhider.client.api.impl.AhPlayerConfigApiImpl;
import de.zannagh.armorhider.configuration.ConfigurationItemFactoryRegistry;
import de.zannagh.armorhider.configuration.items.ArmorOpacity;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class PlayerConfigurationTests {

    @BeforeAll
    static void initializeFactories() {
        ConfigurationItemFactoryRegistry.initialize();
    }

    private static String getVersion3PlayerConfig() {
        return """
                {
                  "helmetOpacity": 0.35,
                  "chestOpacity": 0.35,
                  "legsOpacity": 0.2,
                  "bootsOpacity": 0.25,
                  "playerId": "6f7d35ad-9152-3823-9277-b683a91158a3",
                  "playerName": "Player446",
                  "enableCombatDetection": true
                }""";
    }

    private static String getCurrentVersionPlayerConfig() {
        return """
                {
                  "configVersion": %d,
                  "helmetOpacity": 0.35,
                  "chestOpacity": 0.35,
                  "legsOpacity": 0.2,
                  "bootsOpacity": 0.25,
                  "playerId": "6f7d35ad-9152-3823-9277-b683a91158a3",
                  "playerName": "Player446",
                  "enableCombatDetection": true,
                  "showSettingsInSkinCustomization": true,
                  "inCombatUseDefaultModel": false
                }""".formatted(PlayerConfig.CURRENT_CONFIG_VERSION);
    }

    @Test
    @DisplayName("Read from v1 configuration")
    void readV1() {
        String v1Json = """
                {
                  "helmetTransparency": 0.35,
                  "chestTransparency": 0.35,
                  "legsTransparency": 0.2,
                  "bootsTransparency": 0.25,
                  "playerId": "6f7d35ad-9152-3823-9277-b683a91158a3",
                  "playerName": "Player446"
                }""";
        var configurationProvider = new StringPlayerConfigProvider(v1Json);
        var currentConfig = configurationProvider.load();
        assertEquals(0.35, currentConfig.helmetOpacity.getValue());
        assertEquals(0.35, currentConfig.chestOpacity.getValue());
        assertEquals(0.2, currentConfig.legsOpacity.getValue());
        assertEquals(0.25, currentConfig.bootsOpacity.getValue());
        assertEquals(UUID.fromString("6f7d35ad-9152-3823-9277-b683a91158a3"), currentConfig.playerId.getValue());
        assertEquals("Player446", currentConfig.playerName.getValue());
        assertEquals(currentConfig.enableCombatDetection.getDefaultValue(), currentConfig.enableCombatDetection.getValue());
    }

    @Test
    @DisplayName("Read from v1 configuration")
    void readV2() {
        String v2Json = """
                {
                  "helmetTransparency": 0.35,
                  "chestTransparency": 0.35,
                  "legsTransparency": 0.2,
                  "bootsTransparency": 0.25,
                  "playerId": "6f7d35ad-9152-3823-9277-b683a91158a3",
                  "playerName": "Player446",
                  "enableCombatDetection": true
                }""";
        var configurationProvider = new StringPlayerConfigProvider(v2Json);
        var currentConfig = configurationProvider.load();
        assertEquals(0.35, currentConfig.helmetOpacity.getValue());
        assertEquals(0.35, currentConfig.chestOpacity.getValue());
        assertEquals(0.2, currentConfig.legsOpacity.getValue());
        assertEquals(0.25, currentConfig.bootsOpacity.getValue());
        assertEquals(UUID.fromString("6f7d35ad-9152-3823-9277-b683a91158a3"), currentConfig.playerId.getValue());
        assertEquals("Player446", currentConfig.playerName.getValue());
        assertEquals(true, currentConfig.enableCombatDetection.getValue());
    }

    @Test
    @DisplayName("Read from partly configuration")
    void shouldReplaceMissingValuesWithDefault() {
        String v2JsonMissingBoots = """
                {
                  "helmetTransparency": 0.35,
                  "chestTransparency": 0.35,
                  "legsTransparency": 0.2,
                  "playerId": "6f7d35ad-9152-3823-9277-b683a91158a3",
                  "playerName": "Player446",
                  "enableCombatDetection": true
                }""";
        var configurationProvider = new StringPlayerConfigProvider(v2JsonMissingBoots);
        var currentConfig = configurationProvider.getValue();
        assertEquals(0.35, currentConfig.helmetOpacity.getValue());
        assertEquals(0.35, currentConfig.chestOpacity.getValue());
        assertEquals(0.2, currentConfig.legsOpacity.getValue());
        assertEquals(ArmorOpacity.DEFAULT_OPACITY, currentConfig.bootsOpacity.getValue());
        assertEquals(UUID.fromString("6f7d35ad-9152-3823-9277-b683a91158a3"), currentConfig.playerId.getValue());
        assertEquals("Player446", currentConfig.playerName.getValue());
        assertEquals(true, currentConfig.enableCombatDetection.getValue());
        assertTrue(currentConfig.hasChangedFromSerializedContent());
    }

    @Test
    @DisplayName("Read from v3 configuration")
    void readV3() {
        var configurationProvider = new StringPlayerConfigProvider(getVersion3PlayerConfig());
        var currentConfig = configurationProvider.getValue();
        assertEquals(0.35, currentConfig.helmetOpacity.getValue());
        assertEquals(0.35, currentConfig.chestOpacity.getValue());
        assertEquals(0.2, currentConfig.legsOpacity.getValue());
        assertEquals(0.25, currentConfig.bootsOpacity.getValue());
        assertEquals(UUID.fromString("6f7d35ad-9152-3823-9277-b683a91158a3"), currentConfig.playerId.getValue());
        assertEquals("Player446", currentConfig.playerName.getValue());
        assertEquals(true, currentConfig.enableCombatDetection.getValue());
    }

    @Test
    @DisplayName("Read current configuration with embedded settings toggle")
    void readCurrentConfigWithEmbeddedSettingsToggle() {
        var configurationProvider = new StringPlayerConfigProvider(getCurrentVersionPlayerConfig());
        var currentConfig = configurationProvider.getValue();
        assertEquals(PlayerConfig.CURRENT_CONFIG_VERSION, currentConfig.configVersion);
        assertEquals(true, currentConfig.showSettingsInSkinCustomization.getValue());
    }

    /**
     * Simulates loading a config from before the item exclusion feature (pre-0.10.0-pre.3).
     * All fields except exclusionItems are present.
     */
    private static String getPreExclusionConfig() {
        return """
                {
                  "helmetOpacity": 0.0,
                  "helmetGlint": true,
                  "chestOpacity": 0.0,
                  "chestGlint": true,
                  "legsOpacity": 1.0,
                  "legsGlint": true,
                  "bootsOpacity": 1.0,
                  "bootsGlint": true,
                  "enableCombatDetection": true,
                  "opacityAffectingElytra": true,
                  "opacityAffectingHatOrSkull": true,
                  "disableArmorHider": false,
                  "disableArmorHiderForOthers": false,
                  "usePlayerSettingsWhenUndeterminable": true,
                  "offHandOpacity": 0.0,
                  "playerId": "6f7d35ad-9152-3823-9277-b683a91158a3",
                  "playerName": "Player446"
                }""";
    }

    @Test
    @DisplayName("Upgrade from pre-exclusion config preserves opacity values")
    void upgradeFromPreExclusionConfig() {
        var configurationProvider = new StringPlayerConfigProvider(getPreExclusionConfig());
        var currentConfig = configurationProvider.getValue();

        // Opacity values must be preserved from the old config
        assertEquals(0.0, currentConfig.helmetOpacity.getValue(), "helmet opacity lost on upgrade");
        assertEquals(0.0, currentConfig.chestOpacity.getValue(), "chest opacity lost on upgrade");
        assertEquals(1.0, currentConfig.legsOpacity.getValue());
        assertEquals(1.0, currentConfig.bootsOpacity.getValue());
        assertEquals(0.0, currentConfig.offHandOpacity.getValue(), "offhand opacity lost on upgrade");

        // Glint values preserved
        assertEquals(true, currentConfig.helmetGlint.getValue());
        assertEquals(true, currentConfig.chestGlint.getValue());

        // Boolean settings preserved
        assertEquals(false, currentConfig.disableArmorHider.getValue(), "disableArmorHider changed on upgrade");
        assertEquals(false, currentConfig.disableArmorHiderForOthers.getValue());

        // exclusionItems must not be null
        assertNotNull(currentConfig.exclusionItems, "exclusionItems is null after upgrade");
        // exclusionItems must have default items
        assertFalse(currentConfig.exclusionItems.getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(),
                "exclusionItems HEAD slot is empty after upgrade");
    }

    @Test
    @DisplayName("Upgrade from pre-exclusion config: re-serialized config preserves values")
    void upgradeFromPreExclusionConfigRoundTrip() {
        // Load old config
        var provider = new StringPlayerConfigProvider(getPreExclusionConfig());
        var config = provider.getValue();

        // Simulate what PlayerConfigFileProvider does when hasChanged:
        // save and reload
        provider.saveCurrent();
        var reloaded = provider.load();

        assertEquals(0.0, reloaded.helmetOpacity.getValue(), "helmet opacity lost after round-trip");
        assertEquals(0.0, reloaded.chestOpacity.getValue(), "chest opacity lost after round-trip");
        assertEquals(0.0, reloaded.offHandOpacity.getValue(), "offhand opacity lost after round-trip");
        assertEquals(false, reloaded.disableArmorHider.getValue(), "disableArmorHider changed after round-trip");
        assertNotNull(reloaded.exclusionItems, "exclusionItems null after round-trip");
    }

    @Test
    @DisplayName("Upgrade: exclusionItems field is correctly initialized from constructor during GSON deserialization")
    void upgradeExclusionItemsNotNull() {
        // Direct GSON deserialization (bypasses StringPlayerConfigProvider)
        var config = PlayerConfig.deserialize(getPreExclusionConfig());

        // This is the critical assertion: does GSON's reflective adapter call
        // the no-arg constructor (which sets exclusionItems = defaults())?
        // Or does it leave it null?
        assertNotNull(config.exclusionItems,
                "GSON deserialization left exclusionItems null — constructor not called?");
        assertFalse(config.exclusionItems.getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(),
                "exclusionItems was initialized but has no HEAD items");
    }

    @Test
    @DisplayName("Upgrade: old config opacity values survive full load/save/load cycle")
    void upgradeConfigManagerRoundTrip() {
        var provider = new StringPlayerConfigProvider(getPreExclusionConfig());
        var config = provider.getValue();

        assertEquals(0.0, config.helmetOpacity.getValue(), "helmet opacity wrong after load");
        assertEquals(0.0, config.chestOpacity.getValue(), "chest opacity wrong after load");

        // Save (via provider, avoids Minecraft.getInstance() in ClientConfigManager.save)
        provider.saveCurrent();
        var reloaded = provider.load();

        assertEquals(0.0, reloaded.helmetOpacity.getValue(), "helmet opacity wrong after save/reload");
        assertEquals(0.0, reloaded.chestOpacity.getValue(), "chest opacity wrong after save/reload");
        assertNotNull(reloaded.exclusionItems, "exclusionItems null after save/reload");
    }

    @Test
    @DisplayName("Upgrade: pre-versioning config gets configVersion 0")
    void preVersioningConfigHasVersionZero() {
        var config = PlayerConfig.deserialize(getPreExclusionConfig());
        assertEquals(0, config.configVersion, "old config should have configVersion 0");
    }

    @Test
    @DisplayName("New config gets current configVersion")
    void newConfigHasCurrentVersion() {
        var config = PlayerConfig.defaults(UUID.randomUUID(), "TestPlayer");
        assertEquals(PlayerConfig.CURRENT_CONFIG_VERSION, config.configVersion);
    }

    @Test
    @DisplayName("No-arg constructor (used by GSON) yields configVersion 0")
    void noArgConstructorHasVersionZero() {
        var config = new PlayerConfig();
        assertEquals(0, config.configVersion, "no-arg constructor must leave configVersion at 0 for GSON migration detection");
    }

    @Test
    @DisplayName("Individual per-player override map round-trips through JSON")
    void individualOverridesRoundTrip() {
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Viewer");

        var override = PlayerConfig.defaults(UUID.randomUUID(), "TargetPlayer");
        override.helmetOpacity.setValue(0.1);
        override.chestOpacity.setValue(0.42);
        config.individualConfigurations.putOverride("mc.example.com", "TargetPlayer", override);

        String json = config.toJson();
        var restored = PlayerConfig.deserialize(json);

        assertNotNull(restored.individualConfigurations, "individualConfigurations must survive deserialization");
        var restoredOverride = restored.individualConfigurations.getOverride("mc.example.com", "TargetPlayer");
        assertNotNull(restoredOverride, "the nested per-player override must round-trip");
        assertEquals(0.1, restoredOverride.helmetOpacity.getValue(), "override helmet opacity must round-trip");
        assertEquals(0.42, restoredOverride.chestOpacity.getValue(), "override chest opacity must round-trip");
        assertEquals("TargetPlayer", restoredOverride.playerName.getValue(), "override player name must round-trip");
        assertNull(restored.individualConfigurations.getOverride("mc.example.com", "SomeoneElse"),
                "unrelated players must not gain overrides");
    }

    @Test
    @DisplayName("forNetwork() strips the private override map")
    void forNetworkStripsOverrides() {
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Viewer");
        config.individualConfigurations.putOverride("mc.example.com", "TargetPlayer",
                PlayerConfig.defaults(UUID.randomUUID(), "TargetPlayer"));
        config.useGlobalOverrideForAllPlayers.setValue(true);
        config.globalPlayerOverride = PlayerConfig.defaults(UUID.randomUUID(), "GlobalOverride");

        var network = config.forNetwork();

        assertTrue(network.individualConfigurations.getValue().isEmpty(),
                "the override map must never be transmitted to the server");
        assertNull(network.globalPlayerOverride, "the global override must never be transmitted to the server");
        assertFalse(network.useGlobalOverrideForAllPlayers.getValue(),
                "the global-override flag must not be transmitted to the server");
    }

    @Test
    @DisplayName("Global override config round-trips through JSON")
    void globalOverrideRoundTrips() {
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Viewer");
        config.useGlobalOverrideForAllPlayers.setValue(true);
        var override = PlayerConfig.defaults(UUID.randomUUID(), "GlobalOverride");
        override.helmetOpacity.setValue(0.15);
        override.chestOpacity.setValue(0.65);
        config.globalPlayerOverride = override;

        var restored = PlayerConfig.deserialize(config.toJson());

        assertTrue(restored.useGlobalOverrideForAllPlayers.getValue(), "the global-override flag must round-trip");
        assertNotNull(restored.globalPlayerOverride, "the global override config must round-trip");
        assertEquals(0.15, restored.globalPlayerOverride.helmetOpacity.getValue(),
                "global override helmet opacity must round-trip");
        assertEquals(0.65, restored.globalPlayerOverride.chestOpacity.getValue(),
                "global override chest opacity must round-trip");
        // The nested override must not recurse into its own global override.
        assertNull(restored.globalPlayerOverride.globalPlayerOverride,
                "nested global override must stay null (no infinite nesting)");
    }

    @Test
    @DisplayName("No-arg constructor leaves globalPlayerOverride null (no recursion)")
    void noArgConstructorLeavesGlobalOverrideNull() {
        var config = new PlayerConfig();
        assertNull(config.globalPlayerOverride,
                "globalPlayerOverride must be lazily null so the constructor doesn't recurse");
        assertFalse(config.useGlobalOverrideForAllPlayers.getValue(), "global override defaults to off");
    }

    @Test
    @DisplayName("Migration heals a legacy config that has the global flags set but a null override")
    void migrationSeedsMissingGlobalOverride() {
        // Reproduce the inert legacy state: an older build created the override lazily, so a user who switched
        // "unknown players → global" (usePlayerSettingsWhenUndeterminable = false) or "global for all players"
        // ended up with the flag persisted but globalPlayerOverride == null — which resolved to throwaway
        // vanilla defaults and made the mod appear to do nothing until the config file was deleted.
        var legacy = PlayerConfig.defaults(UUID.randomUUID(), "Player446");
        legacy.configVersion = 8;
        legacy.usePlayerSettingsWhenUndeterminable.setValue(false);
        legacy.helmetOpacity.setValue(0.35);
        legacy.globalPlayerOverride = null;

        var migrated = legacy.ensureSchemaFrom(legacy);

        assertEquals(PlayerConfig.CURRENT_CONFIG_VERSION, migrated.configVersion, "migration must bump the schema version");
        assertNotNull(migrated.globalPlayerOverride,
                "migration must materialise the global override so the flags don't resolve to throwaway defaults");
        assertEquals(ArmorOpacity.DEFAULT_OPACITY, migrated.globalPlayerOverride.helmetOpacity.getValue(),
                "the seeded override must be a default, not a copy of the viewer's own (0.35) settings");
        assertNull(migrated.globalPlayerOverride.globalPlayerOverride,
                "the seeded override must not itself carry a nested global override");
    }

    @Test
    @DisplayName("Migration leaves the global override lazily null when no global mode is enabled")
    void migrationLeavesGlobalOverrideNullWhenNoGlobalModeEnabled() {
        // Default flags: unknowns use own settings, global-for-all off. A null override is never read in this
        // state, so migration must NOT write a default override into every config (keeps the lazy invariant).
        var legacy = PlayerConfig.defaults(UUID.randomUUID(), "Player446");
        legacy.configVersion = 8;
        legacy.helmetOpacity.setValue(0.35);
        legacy.globalPlayerOverride = null;

        var migrated = legacy.ensureSchemaFrom(legacy);

        assertEquals(PlayerConfig.CURRENT_CONFIG_VERSION, migrated.configVersion, "migration must bump the schema version");
        assertNull(migrated.globalPlayerOverride,
                "migration must not seed an override for configs that never enabled a global mode");
    }

    @Test
    @DisplayName("Migration preserves an existing global override rather than reseeding it")
    void migrationPreservesExistingGlobalOverride() {
        var legacy = PlayerConfig.defaults(UUID.randomUUID(), "Player446");
        legacy.configVersion = 8;
        var override = PlayerConfig.defaults(UUID.randomUUID(), "GlobalOverride");
        override.helmetOpacity.setValue(0.12);
        legacy.globalPlayerOverride = override;

        var migrated = legacy.ensureSchemaFrom(legacy);

        assertNotNull(migrated.globalPlayerOverride);
        assertEquals(0.12, migrated.globalPlayerOverride.helmetOpacity.getValue(),
                "an existing override's values must survive migration, not be reseeded to defaults");
    }

    @Test
    @DisplayName("ensureSchemaFrom migrates an outdated PlayerConfig and is a no-op for a current one")
    void ensureSchemaFromMigratesPlayerConfig() {
        // Up-to-date -> no migration, the same instance is returned.
        var current = PlayerConfig.defaults(UUID.randomUUID(), "Viewer");
        assertFalse(current.shouldMigrate(), "a current-version config must not report needing migration");
        assertSame(current, current.ensureSchemaFrom(current), "no-op migration must return the same instance");

        // Outdated -> migrate to the current version, preserving values, and flag the change.
        var old = PlayerConfig.defaults(UUID.randomUUID(), "Viewer");
        old.configVersion = 5;
        old.helmetOpacity.setValue(0.3);
        assertTrue(old.shouldMigrate(), "an older-version config must report needing migration");
        var migrated = old.ensureSchemaFrom(old);
        assertEquals(PlayerConfig.CURRENT_CONFIG_VERSION, migrated.configVersion, "migration must bump the schema version");
        assertEquals(0.3, migrated.helmetOpacity.getValue(), "migration must preserve existing values");
        assertTrue(migrated.hasChangedFromSerializedContent(), "migration must flag the config as changed");
    }

    @Test
    @DisplayName("ensureSchemaFrom migrates an outdated ServerWideSettings")
    void ensureSchemaFromMigratesServerWideSettings() {
        var current = de.zannagh.armorhider.net.packets.ServerWideSettings.defaults();
        assertFalse(current.shouldMigrate());
        assertSame(current, current.ensureSchemaFrom(current));

        var old = new de.zannagh.armorhider.net.packets.ServerWideSettings(); // no-arg leaves configVersion at 0
        old.forceArmorHiderOff.setValue(true);
        assertTrue(old.shouldMigrate());
        var migrated = old.ensureSchemaFrom(old);
        assertEquals(de.zannagh.armorhider.net.packets.ServerWideSettings.CURRENT_CONFIG_VERSION, migrated.configVersion);
        assertTrue(migrated.forceArmorHiderOff.getValue(), "migration must preserve existing values");
    }

    @Test
    @DisplayName("Read from Config Manager")
    void readFromConfigManager() {
        var configManager = new AhPlayerConfigApiImpl(new StringPlayerConfigProvider(getVersion3PlayerConfig()));
        var currentConfig = configManager.getLocalPlayerConfig();
        assertEquals(0.35, currentConfig.helmetOpacity.getValue());
        assertEquals(0.35, currentConfig.chestOpacity.getValue());
        assertEquals(0.2, currentConfig.legsOpacity.getValue());
        assertEquals(0.25, currentConfig.bootsOpacity.getValue());
        assertEquals(UUID.fromString("6f7d35ad-9152-3823-9277-b683a91158a3"), currentConfig.playerId.getValue());
        assertEquals("Player446", currentConfig.playerName.getValue());
        assertEquals(true, currentConfig.enableCombatDetection.getValue());
    }
}
