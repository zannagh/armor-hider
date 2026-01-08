package de.zannagh.armorhider;

import com.google.common.base.Stopwatch;
import de.zannagh.armorhider.resources.ServerConfiguration;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(true, currentConfig.serverWideSettings.enableCombatDetection.getValue());
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
        assertEquals(true, currentConfig.serverWideSettings.enableCombatDetection.getValue());
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
        assertEquals(true, currentConfig.serverWideSettings.enableCombatDetection.getValue());
    }

    @Test
    @DisplayName("Read from v3 configuration and migrate to v4")
    void readV3MigrateToV4() {
        //region json - v3 format with top-level enableCombatDetection
        String v3Json = """
            {
                "playerConfigs": {
                     "da6fa5b1-bd84-361d-8771-656d46818daa": {
                       "helmetOpacity": 0.5,
                       "chestOpacity": 0.75,
                       "legsOpacity": 1.0,
                       "bootsOpacity": 0.25,
                       "playerId": "da6fa5b1-bd84-361d-8771-656d46818daa",
                       "playerName": "TestPlayer",
                       "enableCombatDetection": true
                     }
                },
                "enableCombatDetection": false
            }
            """;
        //endregion
        var configProvider = new StringServerConfigProvider(v3Json);
        var currentConfig = configProvider.getValue();

        assertEquals(0.5, currentConfig.getPlayerConfigOrDefault("TestPlayer").helmetOpacity.getValue());
        assertEquals(0.75, currentConfig.getPlayerConfigOrDefault("TestPlayer").chestOpacity.getValue());
        assertEquals(true, currentConfig.getPlayerConfigOrDefault("TestPlayer").enableCombatDetection.getValue());

        assertNotNull(currentConfig.serverWideSettings);
        assertEquals(false, currentConfig.serverWideSettings.enableCombatDetection.getValue());

        assertTrue(currentConfig.hasChangedFromSerializedContent());
    }

    @Test
    @DisplayName("Read from v4 configuration")
    void readV4() {
        //region json - v4 format with serverWideSettings object
        String v4Json = """
            {
                "playerConfigs": {
                     "da6fa5b1-bd84-361d-8771-656d46818daa": {
                       "helmetOpacity": 0.2,
                       "chestOpacity": 0.4,
                       "legsOpacity": 0.6,
                       "bootsOpacity": 0.8,
                       "playerId": "da6fa5b1-bd84-361d-8771-656d46818daa",
                       "playerName": "ModernPlayer",
                       "enableCombatDetection": false
                     }
                },
                "serverWideSettings": {
                    "enableCombatDetection": true
                }
            }
            """;
        //endregion
        var configProvider = new StringServerConfigProvider(v4Json);
        var currentConfig = configProvider.getValue();

        assertEquals(0.2, currentConfig.getPlayerConfigOrDefault("ModernPlayer").helmetOpacity.getValue());
        assertEquals(0.4, currentConfig.getPlayerConfigOrDefault("ModernPlayer").chestOpacity.getValue());
        assertEquals(false, currentConfig.getPlayerConfigOrDefault("ModernPlayer").enableCombatDetection.getValue());

        assertNotNull(currentConfig.serverWideSettings);
        assertEquals(true, currentConfig.serverWideSettings.enableCombatDetection.getValue());

        assertTrue(currentConfig.hasChangedFromSerializedContent());
    }

    @Test
    @DisplayName("Compressed packet size test - up to 500 players")
    void testCompressedPacketSizes() {
        int[] playerCounts = {1, 10, 50, 100, 200, 300, 400, 500, 1000, 1500};
        int maxReasonableByteSize = 2 * 1024 * 1024;

        HashMap<Integer, Boolean> sizeAcceptable = new HashMap<>();
        for (int playerCount : playerCounts) {
            
            StringServerConfigProvider provider = ServerConfigProviderMock.createServerConfigWithPlayers(playerCount);
            ServerConfiguration config = provider.getValue();

            String json = de.zannagh.armorhider.ArmorHider.GSON.toJson(config);
            int uncompressedSize = json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;

            ByteBuf buffer = Unpooled.buffer();
            try {
                Stopwatch stopwatch = Stopwatch.createStarted();
                config.getCodec().encode(buffer, config);
                stopwatch.stop();
                int compressedSize = buffer.readableBytes();
                double compressionRatio = (double) uncompressedSize / compressedSize;

                String uncompressedStr = formatBytes(uncompressedSize);
                String compressedStr = formatBytes(compressedSize);
                System.out.printf("[%3d players] %8s → %8s (%.1fx compression). Encoding elapsed: %s ms%n",
                        playerCount, uncompressedStr, compressedStr, compressionRatio, stopwatch.elapsed(TimeUnit.MILLISECONDS));
                sizeAcceptable.put(playerCount, compressedSize < maxReasonableByteSize);

            } finally {
                buffer.release();
            }
        }
        
        boolean allSizesAcceptable = true;

        for (var entry : sizeAcceptable.entrySet()) {
            if (!entry.getValue()) {
                allSizesAcceptable = false;
                break;
            }
        }
        assertTrue(allSizesAcceptable, "All packet sizes within acceptable limits");
    }

    private @NonNull String formatBytes(int bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else {
            return String.format("%.2fMB", bytes / (1024.0 * 1024.0));
        }
    }
}
