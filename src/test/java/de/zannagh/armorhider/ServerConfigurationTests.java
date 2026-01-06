package de.zannagh.armorhider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerConfigurationTests {
    @Test
    @DisplayName("Read from v1 configuration")
    void readV1() {
        //region json
        String v1Json = """
                {
                    "ab5af6cc-0975-34e7-baca-b908d8aa661c": {
                       "helmetTransparency": 0.3,
                       "chestTransparency": 0.0,
                       "legsTransparency": 0.0,
                       "bootsTransparency": 0.0,
                       "playerId": "ab5af6cc-0975-34e7-baca-b908d8aa661c",
                       "playerName": "Player354"
                    },
                    "81ef6cf6-107d-35d0-bf6b-c2f04f3d07c4": {
                       "helmetTransparency": 1.0,
                       "chestTransparency": 1.0,
                       "legsTransparency": 0.25,
                       "bootsTransparency": 1.0,
                       "playerId": "81ef6cf6-107d-35d0-bf6b-c2f04f3d07c4",
                       "playerName": "Player354"
                    },
                    "94cce2b9-df97-3b51-a97b-9838f894b678": {
                       "helmetTransparency": 0.1,
                       "chestTransparency": 0.2,
                       "legsTransparency": 0.05,
                       "bootsTransparency": 1.0,
                       "playerId": "94cce2b9-df97-3b51-a97b-9838f894b678",
                       "playerName": "Player393"
                    }
                }""";
        //endregion
        var configProvider = new StringServerConfigProvider(v1Json);
        var currentConfig = configProvider.getValue();
        assertEquals(0.1, currentConfig.getPlayerConfigOrDefault("Player393").helmetOpacity.getValue());
        assertEquals(0.3, currentConfig.getPlayerConfigOrDefault(UUID.fromString("ab5af6cc-0975-34e7-baca-b908d8aa661c")).helmetOpacity.getValue());
        assertEquals(true, currentConfig.getPlayerConfigOrDefault("Player393").enableCombatDetection.getValue());
        assertEquals(true, currentConfig.getPlayerConfigOrDefault(UUID.fromString("ab5af6cc-0975-34e7-baca-b908d8aa661c")).enableCombatDetection.getValue());
        assertEquals(true, currentConfig.enableCombatDetection.getValue());
    }

    @Test
    @DisplayName("Read from v2 configuration")
    void readV2() {
        //region json
        String v2Json = """
            {
                "playerConfigs": {
                     "da6fa5b1-bd84-361d-8771-656d46818daa": {
                       "helmetTransparency": 0.3,
                       "chestTransparency": 0.55,
                       "legsTransparency": 1.0,
                       "bootsTransparency": 0.0,
                       "playerId": "da6fa5b1-bd84-361d-8771-656d46818daa",
                       "playerName": "Player706",
                       "enableCombatDetection": false
                     },
                     "81ef6cf6-107d-35d0-bf6b-c2f04f3d07c4": {
                       "helmetTransparency": 1.0,
                       "chestTransparency": 1.0,
                       "legsTransparency": 0.25,
                       "bootsTransparency": 1.0,
                       "playerId": "81ef6cf6-107d-35d0-bf6b-c2f04f3d07c4",
                       "playerName": "Player354",
                       "enableCombatDetection": false
                     },
                     "94cce2b9-df97-3b51-a97b-9838f894b678": {
                       "helmetTransparency": 0.1,
                       "chestTransparency": 0.2,
                       "legsTransparency": 0.05,
                       "bootsTransparency": 1.0,
                       "playerId": "94cce2b9-df97-3b51-a97b-9838f894b678",
                       "playerName": "Player393",
                       "enableCombatDetection": false
                     }
                },
                "playerNameConfigs": {
                     "Player706": {
                       "helmetTransparency": 0.3,
                       "chestTransparency": 0.55,
                       "legsTransparency": 1.0,
                       "bootsTransparency": 0.0,
                       "playerId": "da6fa5b1-bd84-361d-8771-656d46818daa",
                       "playerName": "Player706",
                       "enableCombatDetection": false
                     },
                     "Player354": {
                       "helmetTransparency": 1.0,
                       "chestTransparency": 1.0,
                       "legsTransparency": 0.25,
                       "bootsTransparency": 1.0,
                       "playerId": "81ef6cf6-107d-35d0-bf6b-c2f04f3d07c4",
                       "playerName": "Player354",
                       "enableCombatDetection": false
                     },
                     "Player393": {
                       "helmetTransparency": 0.1,
                       "chestTransparency": 0.2,
                       "legsTransparency": 0.05,
                       "bootsTransparency": 1.0,
                       "playerId": "94cce2b9-df97-3b51-a97b-9838f894b678",
                       "playerName": "Player393",
                       "enableCombatDetection": false
                     }
                },
                "enableCombatDetection": true
            }
            """;
        //endregion
        var configProvider = new StringServerConfigProvider(v2Json);
        var currentConfig = configProvider.getValue();
        assertEquals(0.1, currentConfig.getPlayerConfigOrDefault("Player393").helmetOpacity.getValue());
        assertEquals(0.3, currentConfig.getPlayerConfigOrDefault(UUID.fromString("da6fa5b1-bd84-361d-8771-656d46818daa")).helmetOpacity.getValue());
        assertEquals(false, currentConfig.getPlayerConfigOrDefault("Player393").enableCombatDetection.getValue());
        assertEquals(false, currentConfig.getPlayerConfigOrDefault(UUID.fromString("da6fa5b1-bd84-361d-8771-656d46818daa")).enableCombatDetection.getValue());
        assertEquals(true, currentConfig.enableCombatDetection.getValue());
    }

    @Test
    @DisplayName("Read from partly configuration")
    void readPartlyConfig() {
        //region json
        String v2Json = """
            {
                "playerConfigs": {
                     "da6fa5b1-bd84-361d-8771-656d46818daa": {
                       "helmetTransparency": 0.3,
                       "chestTransparency": 0.55,
                       "legsTransparency": 1.0,
                       "bootsTransparency": 0.0,
                       "playerId": "da6fa5b1-bd84-361d-8771-656d46818daa",
                       "playerName": "Player706",
                       "enableCombatDetection": false
                     },
                     "81ef6cf6-107d-35d0-bf6b-c2f04f3d07c4": {
                       "helmetTransparency": 1.0,
                       "chestTransparency": 1.0,
                       "legsTransparency": 0.25,
                       "bootsTransparency": 1.0,
                       "playerId": "81ef6cf6-107d-35d0-bf6b-c2f04f3d07c4",
                       "playerName": "Player354"
                     },
                     "94cce2b9-df97-3b51-a97b-9838f894b678": {
                       "helmetTransparency": 0.1,
                       "chestTransparency": 0.2,
                       "legsTransparency": 0.05,
                       "bootsTransparency": 1.0,
                       "playerId": "94cce2b9-df97-3b51-a97b-9838f894b678",
                       "playerName": "Player393"
                     }
                },
                "playerNameConfigs": {
                     "Player706": {
                       "helmetTransparency": 0.3,
                       "chestTransparency": 0.55,
                       "legsTransparency": 1.0,
                       "bootsTransparency": 0.0,
                       "playerId": "da6fa5b1-bd84-361d-8771-656d46818daa",
                       "playerName": "Player706",
                       "enableCombatDetection": false
                     },
                     "Player354": {
                       "helmetTransparency": 1.0,
                       "chestTransparency": 1.0,
                       "legsTransparency": 0.25,
                       "bootsTransparency": 1.0,
                       "playerId": "81ef6cf6-107d-35d0-bf6b-c2f04f3d07c4",
                       "playerName": "Player354"
                     },
                     "Player393": {
                       "helmetTransparency": 0.1,
                       "chestTransparency": 0.2,
                       "legsTransparency": 0.05,
                       "bootsTransparency": 1.0,
                       "playerId": "94cce2b9-df97-3b51-a97b-9838f894b678",
                       "playerName": "Player393"
                     }
                }
            }
            """;
        //endregion
        var configProvider = new StringServerConfigProvider(v2Json);
        var currentConfig = configProvider.getValue();
        assertEquals(0.1, currentConfig.getPlayerConfigOrDefault("Player393").helmetOpacity.getValue());
        assertEquals(0.3, currentConfig.getPlayerConfigOrDefault(UUID.fromString("da6fa5b1-bd84-361d-8771-656d46818daa")).helmetOpacity.getValue());
        assertEquals(true, currentConfig.getPlayerConfigOrDefault("Player393").enableCombatDetection.getValue());
        assertEquals(false, currentConfig.getPlayerConfigOrDefault(UUID.fromString("da6fa5b1-bd84-361d-8771-656d46818daa")).enableCombatDetection.getValue());
        assertEquals(true, currentConfig.enableCombatDetection.getValue());
    }
}
