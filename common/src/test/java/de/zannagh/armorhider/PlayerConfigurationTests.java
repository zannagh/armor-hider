package de.zannagh.armorhider;

import de.zannagh.armorhider.client.api.impl.AhPlayerConfigApiImpl;
import de.zannagh.armorhider.configuration.ConfigurationItemFactoryRegistry;
import de.zannagh.armorhider.configuration.ExclusionItemConfiguration;
import de.zannagh.armorhider.net.CompressedJsonCodec;
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

    // ── Config healing (schema v12) ──────────────────────────────────────────────────────────────
    // These cover the corruption that survives a restart AND a mod reinstall, because it lives in
    // config/armor-hider.json rather than in the mod. Healing runs on every deserialize, NOT only when
    // the version is stale — a config already at the current version can still be corrupt.

    @Test
    @DisplayName("Opacity above the valid range is clamped when read from disk")
    void healClampsOpacityAboveRange() {
        var config = PlayerConfig.deserialize("{\"configVersion\": 12, \"helmetOpacity\": 5.0}");
        assertEquals(1.0, config.helmetOpacity.getValue(),
                "opacity must clamp to 1.0; the Gson read path uses the value constructor, not setValue");
    }

    @Test
    @DisplayName("Negative opacity is clamped when read from disk")
    void healClampsNegativeOpacity() {
        var config = PlayerConfig.deserialize("{\"configVersion\": 12, \"chestOpacity\": -3.0}");
        assertEquals(0.0, config.chestOpacity.getValue());
    }

    @Test
    @DisplayName("Non-finite opacity falls back to the default and stays serializable")
    void healRejectsNonFiniteOpacity() {
        // Bare NaN/Infinity are not legal JSON, so they cannot arrive through the parser. They reach a config
        // item through the single-argument constructor — which is exactly the path the Gson type adapter uses
        // — or through arithmetic on a value. Either way the danger is the same: Gson#toJson throws
        // IllegalArgumentException on a non-finite double, which escapes the IOException-only catch in the
        // save path and can leave the settings screen unclosable.
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Corrupt");
        config.legsOpacity = new ArmorOpacity(Double.NaN);
        assertEquals(ArmorOpacity.DEFAULT_OPACITY, config.legsOpacity.getValue(),
                "the value constructor must reject NaN, since the Gson read path never calls setValue");
        assertEquals(ArmorOpacity.DEFAULT_OPACITY, new ArmorOpacity(Double.POSITIVE_INFINITY).getValue());
        assertDoesNotThrow(config::toJson, "a healed config must always be serializable");
    }

    @Test
    @DisplayName("Setting an opacity out of range clamps it too")
    void setValueClampsOpacity() {
        var opacity = new ArmorOpacity();
        opacity.setValue(42.0);
        assertEquals(1.0, opacity.getValue());
        opacity.setValue(Double.NEGATIVE_INFINITY);
        assertEquals(ArmorOpacity.DEFAULT_OPACITY, opacity.getValue());
    }

    @Test
    @DisplayName("Healing strips synthetic unknown: exclusion IDs, which can never match again")
    void healPrunesSyntheticExclusionIds() {
        String json = """
                {
                  "configVersion": 12,
                  "exclusionItems": {
                    "items": {
                      "HEAD": {
                        "minecraft:iron_helmet": {"displayName": "Iron Helmet", "shouldIgnore": false},
                        "unknown:someitem_12345": {"displayName": "Mystery", "shouldIgnore": false},
                        "unknown:someitem_67890": {"displayName": "Mystery", "shouldIgnore": false}
                      }
                    }
                  }
                }
                """;
        var config = PlayerConfig.deserialize(json);
        var head = config.getExclusionItems()
                .getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        assertTrue(head.containsKey("minecraft:iron_helmet"), "real registry IDs must be kept");
        assertEquals(1, head.size(), "synthetic identity-hash IDs must be dropped");
        assertTrue(config.hasChangedFromSerializedContent(),
                "pruning must mark the config dirty so the repaired form is written back");
    }

    @Test
    @DisplayName("Healing caps discovered exclusion entries but keeps user-excluded ones")
    void healCapsDiscoveredExclusionsButKeepsUserIntent() {
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Hoarder");
        var exclusions = config.getExclusionItems();
        int overCap = ExclusionItemConfiguration.MAX_DISCOVERED_ITEMS_PER_SLOT + 50;
        for (int i = 0; i < overCap; i++) {
            exclusions.setItem(net.minecraft.world.entity.EquipmentSlot.HEAD, "testmod:helmet_" + i,
                    de.zannagh.armorhider.configuration.ExclusionItemInfo.intercepted("Helmet " + i));
        }
        exclusions.setItem(net.minecraft.world.entity.EquipmentSlot.HEAD, "testmod:user_excluded",
                de.zannagh.armorhider.configuration.ExclusionItemInfo.ignored("Deliberately excluded"));

        exclusions.prune();

        var head = exclusions.getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        assertTrue(head.containsKey("testmod:user_excluded"),
                "an entry the user deliberately excluded must never be pruned away");
        assertTrue(head.size() <= ExclusionItemConfiguration.MAX_DISCOVERED_ITEMS_PER_SLOT + 1,
                "discovered entries must be capped, got " + head.size());
        assertFalse(head.containsKey("testmod:helmet_0"), "the oldest discovered entries are dropped first");
    }

    @Test
    @DisplayName("Healing flattens a recursively nested global override")
    void healFlattensNestedGlobalOverride() {
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Viewer");
        config.globalPlayerOverride = PlayerConfig.defaults(UUID.randomUUID(), "Global");
        config.globalPlayerOverride.globalPlayerOverride =
                PlayerConfig.defaults(UUID.randomUUID(), "TooDeep");

        PlayerConfig.heal(config);

        assertNotNull(config.globalPlayerOverride, "one level of override is legitimate and must be kept");
        assertNull(config.globalPlayerOverride.globalPlayerOverride,
                "deeper nesting is corruption and must be dropped before it can break serialization");
    }

    @Test
    @DisplayName("forNetwork drops the exclusion map so the C2S payload stays under the vanilla limit")
    void forNetworkDropsExclusionItems() {
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Sender");
        var exclusions = config.getExclusionItems();
        for (int i = 0; i < 2000; i++) {
            exclusions.setItem(net.minecraft.world.entity.EquipmentSlot.CHEST, "testmod:plate_" + i,
                    de.zannagh.armorhider.configuration.ExclusionItemInfo.intercepted("Plate number " + i));
        }

        var network = config.forNetwork();
        assertFalse(network.getExclusionItems()
                        .getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                        .containsKey("testmod:plate_0"),
                "the client-only exclusion map must not be broadcast");

        int encodedSize = network.toJson().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        assertTrue(encodedSize < CompressedJsonCodec.MAX_SERVERBOUND_PAYLOAD_BYTES,
                "even uncompressed, the network config must sit well under the 32767-byte serverbound limit, got "
                        + encodedSize);
    }

    @Test
    @DisplayName("forNetwork sends an empty exclusion map, not the seeded defaults")
    void forNetworkSendsEmptyExclusionMap() {
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Sender");
        var network = config.forNetwork();
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            assertTrue(network.getExclusionItems().getItemsForSlot(slot).isEmpty(),
                    "slot " + slot + " should carry no exclusion entries over the wire");
        }
        // Behaviourally identical to sending ExclusionItemConfiguration.defaults(): every default entry is
        // `intercepted` (shouldIgnore == false), and shouldArmorHiderIgnore also returns false for an absent
        // entry — so an empty map resolves the same way for every reader while being smaller on the wire.
        // (Not asserted through shouldArmorHiderIgnore directly: that needs an Item instance, and touching
        // net.minecraft.world.item.Items requires the MC registry bootstrap these unit tests do not run.)
    }

    @Test
    @DisplayName("Healing survives nulls inside the exclusion map instead of wiping the config")
    void healToleratesNullExclusionEntries() {
        // exclusionItems is a plain reflective Gson map, NOT covered by
        // ConfigurationSourceSerializer#initializeNullConfigFields, so explicit JSON nulls reach prune()
        // verbatim. An NPE there escapes deserialize() into PlayerConfigFileProvider's catch-all, which
        // throws the config away and writes defaults — the exact opposite of healing it.
        String json = """
                {
                  "configVersion": 12,
                  "helmetOpacity": 0.5,
                  "exclusionItems": {
                    "items": {
                      "HEAD": null,
                      "CHEST": {"minecraft:iron_chestplate": null, "minecraft:elytra": {"displayName": "Elytra", "shouldIgnore": true}}
                    }
                  }
                }
                """;
        var config = assertDoesNotThrow(() -> PlayerConfig.deserialize(json),
                "healing must tolerate a corrupt exclusion map rather than throwing");

        assertEquals(0.5, config.helmetOpacity.getValue(), "unrelated settings must survive the repair");
        assertTrue(config.getExclusionItems()
                        .getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(),
                "a null slot map must be dropped, and getItemsForSlot must never hand back null");
        var chest = config.getExclusionItems()
                .getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        assertFalse(chest.containsKey("minecraft:iron_chestplate"), "a null entry must be dropped");
        assertTrue(chest.containsKey("minecraft:elytra"), "valid neighbours must be preserved");
    }

    @Test
    @DisplayName("Discovery order survives a save/reload, so prune still drops oldest-first")
    void exclusionPruneKeepsDiscoveryOrderAcrossReload() {
        // Insert discovered keys in DESCENDING numeric order, so insertion (discovery) order and sorted order
        // disagree: discovery-first is "k511", sorted-first is "k000". If the map deserialized as a sorted
        // structure, prune would drop "k000"; if discovery order is preserved, it drops "k511".
        var original = new ExclusionItemConfiguration();
        int count = ExclusionItemConfiguration.MAX_DISCOVERED_ITEMS_PER_SLOT + 1; // one over the cap
        for (int i = count - 1; i >= 0; i--) {
            original.setItem(net.minecraft.world.entity.EquipmentSlot.HEAD,
                    String.format("testmod:k%03d", i),
                    de.zannagh.armorhider.configuration.ExclusionItemInfo.intercepted("k" + i));
        }

        // Round-trip through JSON exactly as a real config would on save + next launch.
        var reloaded = ExclusionItemConfiguration.deserialize(
                de.zannagh.armorhider.ArmorHider.GSON.toJson(original));

        var beforePrune = new java.util.ArrayList<>(
                reloaded.getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.HEAD).keySet());
        assertEquals("testmod:k512", beforePrune.get(0),
                "the first-discovered key must still iterate first after a reload (insertion order, not sorted)");

        reloaded.prune();

        var head = reloaded.getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        assertEquals(ExclusionItemConfiguration.MAX_DISCOVERED_ITEMS_PER_SLOT, head.size());
        assertFalse(head.containsKey("testmod:k512"), "the oldest-discovered entry must be the one dropped");
        assertTrue(head.containsKey("testmod:k000"), "the newest-discovered entry must be kept");
    }

    @Test
    @DisplayName("Healing a self-referential global override does not throw")
    void healToleratesSelfReferentialGlobalOverride() {
        // Not reachable from JSON (a tree), but heal() is public API and callable on programmatically-built
        // instances. A naive re-read of config.globalPlayerOverride after recursing would NPE here, because
        // the recursive call nulls that very field on the shared instance when it hits the depth limit.
        var config = PlayerConfig.defaults(UUID.randomUUID(), "SelfRef");
        config.globalPlayerOverride = config;

        var healed = assertDoesNotThrow(() -> PlayerConfig.heal(config),
                "healing must never itself throw, even on a self-referential structure");
        assertNull(healed.globalPlayerOverride,
                "the self-reference exceeds the one meaningful level and must be dropped");
    }

    @Test
    @DisplayName("An items:null exclusion map is repaired AND marked for persistence")
    void healPersistsNullItemsMapRepair() {
        // prune() must report the null-map materialisation as a repair (> 0), otherwise heal() never flags the
        // config dirty and the "items": null corruption is re-read and re-repaired on every launch, forever.
        var exclusions = ExclusionItemConfiguration.deserialize("{\"items\": null}");
        assertTrue(exclusions.prune() > 0, "materialising a null backing map must count as a repair");
        // Idempotent: a second pass over the now-healthy map reports nothing to do.
        assertEquals(0, exclusions.prune(), "a healthy map needs no further repair");
        assertTrue(exclusions.getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty());
    }

    @Test
    @DisplayName("deepCopy tolerates a corrupt exclusion map (presets.json is never healed)")
    void deepCopyToleratesNullExclusionEntries() {
        var source = ExclusionItemConfiguration.deserialize(
                "{\"items\": {\"HEAD\": null, \"CHEST\": {\"minecraft:elytra\": null}}}");
        var copy = assertDoesNotThrow(source::deepCopy);
        assertTrue(copy.getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty());
        assertTrue(copy.getItemsForSlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty());
    }

    @Test
    @DisplayName("updateActivePreset materialises a missing active preset instead of dropping the edit")
    void updateActivePresetMaterialisesMissingPreset(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        // activeIndex pointing at a null preset is reachable from a hand-edited or partially written file.
        var presetFile = tempDir.resolve("presets.json");
        java.nio.file.Files.writeString(presetFile,
                "{\"presets\": [null, null, null, null, null], \"activeIndex\": 0}");

        var manager = new de.zannagh.armorhider.configuration.PresetManager(presetFile);
        assertNull(manager.getPreset(0), "precondition: the active slot must start empty");

        var config = PlayerConfig.defaults(UUID.randomUUID(), "Editor");
        config.helmetOpacity.setValue(0.4);
        manager.updateActivePreset(config);
        manager.flushPendingSave();

        var preset = manager.getPreset(0);
        assertNotNull(preset, "the edit must materialise the missing preset rather than being discarded");
        assertEquals(0.4, preset.helmetOpacity);
        assertTrue(java.nio.file.Files.readString(presetFile).contains("0.4"),
                "the materialised preset must be persisted");
    }

    @Test
    @DisplayName("A failed preset save stays pending and is retried by the next flush")
    void failedPresetSaveIsRetried(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir)
            throws Exception {
        // Block the parent directory with a regular file so Files.createDirectories fails and save() throws.
        var blocker = tempDir.resolve("presets");
        java.nio.file.Files.createFile(blocker);
        var presetFile = blocker.resolve("presets.json");

        var manager = new de.zannagh.armorhider.configuration.PresetManager(presetFile);
        manager.setActiveIndex(0);
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Editor");
        config.helmetOpacity.setValue(0.25);
        manager.updateActivePreset(config);

        manager.flushPendingSave();
        assertFalse(java.nio.file.Files.exists(presetFile), "precondition: the first write must have failed");

        // Unblock and flush again. If the failed attempt had cleared the pending flag, this second flush
        // would be a no-op and the user's edit would be lost with no way to recover it.
        java.nio.file.Files.delete(blocker);
        manager.flushPendingSave();

        assertTrue(java.nio.file.Files.exists(presetFile),
                "a failed save must remain pending so the next flush retries it");
        assertTrue(java.nio.file.Files.readString(presetFile).contains("0.25"),
                "the retried write must contain the edit that was pending");
    }

    @Test
    @DisplayName("A config already at the current version is still healed")
    void healRunsEvenWhenVersionIsCurrent() {
        String json = "{\"configVersion\": " + PlayerConfig.CURRENT_CONFIG_VERSION
                + ", \"bootsOpacity\": 99.0}";
        var config = PlayerConfig.deserialize(json);
        assertFalse(config.shouldMigrate(), "precondition: this config must not qualify for migration");
        assertEquals(1.0, config.bootsOpacity.getValue(),
                "healing must not be gated on the schema version — that is why the reporter's config survived a reinstall");
    }
}
