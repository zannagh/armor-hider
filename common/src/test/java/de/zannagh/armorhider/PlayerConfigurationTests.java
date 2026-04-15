package de.zannagh.armorhider;

import de.zannagh.armorhider.client.ClientConfigManager;
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
                  "configVersion": 2,
                  "helmetOpacity": 0.35,
                  "chestOpacity": 0.35,
                  "legsOpacity": 0.2,
                  "bootsOpacity": 0.25,
                  "playerId": "6f7d35ad-9152-3823-9277-b683a91158a3",
                  "playerName": "Player446",
                  "enableCombatDetection": true,
                  "showSettingsInSkinCustomization": true
                }""";
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
    @DisplayName("Read from Config Manager")
    void readFromConfigManager() {
        ClientConfigManager configManager = new ClientConfigManager(new StringPlayerConfigProvider(getVersion3PlayerConfig()));
        var currentConfig = configManager.getValue();
        assertEquals(0.35, currentConfig.helmetOpacity.getValue());
        assertEquals(0.35, currentConfig.chestOpacity.getValue());
        assertEquals(0.2, currentConfig.legsOpacity.getValue());
        assertEquals(0.25, currentConfig.bootsOpacity.getValue());
        assertEquals(UUID.fromString("6f7d35ad-9152-3823-9277-b683a91158a3"), currentConfig.playerId.getValue());
        assertEquals("Player446", currentConfig.playerName.getValue());
        assertEquals(true, currentConfig.enableCombatDetection.getValue());
    }
}
