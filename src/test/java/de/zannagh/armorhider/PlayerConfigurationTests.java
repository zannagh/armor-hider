package de.zannagh.armorhider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.configuration.items.implementations.ArmorOpacity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;


class PlayerConfigurationTests {
    
    @Test
    @DisplayName("Read from v1 configuration")
    void readV1(){
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
    void readV2(){
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
    void shouldReplaceMissingValuesWithDefault(){
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
    void readV3(){
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
    @DisplayName("Read from Config Manager")
    void readFromConfigManager(){
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
}
