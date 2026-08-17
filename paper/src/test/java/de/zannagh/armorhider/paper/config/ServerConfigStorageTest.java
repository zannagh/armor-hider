package de.zannagh.armorhider.paper.config;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Disk round-trip and legacy-format migration for {@link ServerConfigStorage}. Exercised end to end
 * against a real (temp-dir) filesystem, so both the happy path and every recovery branch - missing
 * file, corrupt file, the v1/v2/v3 on-disk shapes - are covered.
 */
@DisplayName("ServerConfigStorage disk I/O and migration")
class ServerConfigStorageTest {

    private static final Logger SILENT = Logger.getLogger(ServerConfigStorageTest.class.getName());
    private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private ServerConfigStorage storage(Path dir) {
        return new ServerConfigStorage(dir.resolve("armor-hider.json"),
                dir.resolve("config").resolve("armor-hider-server.json"), SILENT);
    }

    private static JsonObject playerConfig(String name, UUID id) {
        JsonObject config = new JsonObject();
        config.addProperty(ServerConfigurationState.PLAYER_NAME, name);
        config.addProperty(ServerConfigurationState.PLAYER_ID, id.toString());
        config.addProperty("helmetOpacity", 0.5);
        return config;
    }

    @Test
    @DisplayName("load() on a missing file writes and returns a default state")
    void loadMissingCreatesDefault(@TempDir Path dir) {
        ServerConfigStorage storage = storage(dir);
        ServerConfigurationState state = storage.load();

        assertNotNull(state);
        assertTrue(state.getPlayerConfigs().isEmpty());
        assertTrue(Files.exists(dir.resolve("armor-hider.json")), "a fresh config must have been written");
    }

    @Test
    @DisplayName("save() then load() round-trips player configs and server-wide settings")
    void saveLoadRoundTrips(@TempDir Path dir) {
        ServerConfigStorage storage = storage(dir);
        ServerConfigurationState state = new ServerConfigurationState();
        state.put(PLAYER, playerConfig("Zannagh", PLAYER));
        JsonObject settings = ServerWideSettingsDefaults.create();
        settings.addProperty(ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF, true);
        state.setServerWideSettings(settings);

        storage.save(state);
        ServerConfigurationState reloaded = storage.load();

        assertEquals(1, reloaded.getPlayerConfigs().size());
        assertEquals("Zannagh",
                ServerConfigurationState.readPlayerName(reloaded.getPlayerConfigs().get(PLAYER)));
        assertTrue(reloaded.getPlayerNameConfigs().containsKey("Zannagh"),
                "the by-name index must survive the disk round-trip");
        assertTrue(ServerWideSettingsDefaults.readBoolean(reloaded.getServerWideSettings(),
                ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF));
    }

    @Test
    @DisplayName("load() on a corrupt file falls back to defaults instead of throwing")
    void loadCorruptFallsBack(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("armor-hider.json"), "not json at all {");
        ServerConfigurationState state = storage(dir).load();
        assertNotNull(state);
        assertTrue(state.getPlayerConfigs().isEmpty());
    }

    @Test
    @DisplayName("load() migrates the legacy v1/v2 flat UUID->config map")
    void loadMigratesFlatMap(@TempDir Path dir) throws IOException {
        JsonObject flat = new JsonObject();
        flat.add(PLAYER.toString(), playerConfig("LegacyPlayer", PLAYER));
        Files.writeString(dir.resolve("armor-hider.json"), flat.toString());

        ServerConfigurationState state = storage(dir).load();

        assertEquals(1, state.getPlayerConfigs().size());
        assertEquals("LegacyPlayer",
                ServerConfigurationState.readPlayerName(state.getPlayerConfigs().get(PLAYER)));
    }

    @Test
    @DisplayName("load() migrates a v3 root enableCombatDetection into serverWideSettings")
    void loadMigratesV3CombatFlag(@TempDir Path dir) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("enableCombatDetection", false);
        JsonObject byId = new JsonObject();
        byId.add(PLAYER.toString(), playerConfig("V3Player", PLAYER));
        root.add("playerConfigs", byId);
        Files.writeString(dir.resolve("armor-hider.json"), root.toString());

        ServerConfigurationState state = storage(dir).load();

        assertFalse(ServerWideSettingsDefaults.readBoolean(state.getServerWideSettings(),
                ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION));
        assertEquals(1, state.getPlayerConfigs().size());
    }

    @Test
    @DisplayName("load() keys a config by its embedded playerId when the map key is not a UUID")
    void loadUsesEmbeddedPlayerIdWhenKeyNotUuid(@TempDir Path dir) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject byId = new JsonObject();
        byId.add("not-a-uuid", playerConfig("EmbeddedId", PLAYER));
        root.add("playerConfigs", byId);
        Files.writeString(dir.resolve("armor-hider.json"), root.toString());

        ServerConfigurationState state = storage(dir).load();

        assertTrue(state.getPlayerConfigs().containsKey(PLAYER),
                "the embedded playerId should be used when the map key is unparseable");
    }

    @Test
    @DisplayName("migrateGlobalConfigIfNeeded copies the legacy file only when the world has none")
    void migratesGlobalConfig(@TempDir Path dir) throws IOException {
        Path legacy = dir.resolve("config").resolve("armor-hider-server.json");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "{\"playerConfigs\":{}}");

        ServerConfigStorage storage = storage(dir);
        storage.migrateGlobalConfigIfNeeded();
        assertTrue(Files.exists(dir.resolve("armor-hider.json")), "legacy config must be copied into the world");

        // A second run must not clobber the now-present world config.
        Files.writeString(dir.resolve("armor-hider.json"), "{\"marker\":true,\"playerConfigs\":{}}");
        storage.migrateGlobalConfigIfNeeded();
        assertTrue(Files.readString(dir.resolve("armor-hider.json")).contains("marker"),
                "an existing world config must never be overwritten by the legacy copy");
    }

    @Test
    @DisplayName("migrateDimensionFolderConfigIfNeeded moves a stranded config to the level root")
    void migratesStrandedDimensionConfig(@TempDir Path dir) throws IOException {
        Path stranded = dir.resolve("dimensions").resolve("minecraft").resolve("overworld")
                .resolve("armor-hider.json");
        Files.createDirectories(stranded.getParent());
        Files.writeString(stranded, "{\"playerConfigs\":{}}");

        storage(dir).migrateDimensionFolderConfigIfNeeded(stranded);

        assertTrue(Files.exists(dir.resolve("armor-hider.json")), "config must be moved to the level root");
        assertFalse(Files.exists(stranded), "the stranded copy must be gone after the move");
    }

    @Test
    @DisplayName("migrateDimensionFolderConfigIfNeeded is a no-op when the canonical file already exists")
    void dimensionMigrationSkipsWhenCanonicalExists(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("armor-hider.json"), "{\"canonical\":true,\"playerConfigs\":{}}");
        Path stranded = dir.resolve("dimensions").resolve("armor-hider.json");
        Files.createDirectories(stranded.getParent());
        Files.writeString(stranded, "{\"stranded\":true,\"playerConfigs\":{}}");

        storage(dir).migrateDimensionFolderConfigIfNeeded(stranded);

        assertTrue(Files.readString(dir.resolve("armor-hider.json")).contains("canonical"),
                "current state must win over the stranded copy");
        assertTrue(Files.exists(stranded), "the stranded copy is left untouched when it is not adopted");
    }
}
