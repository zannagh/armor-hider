package de.zannagh.armorhider.paper.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In-memory protocol state: name-collision reconciliation, the clientbound document shape, and the
 * null-safe field readers that keep a schema-agnostic relay from throwing on a malformed config.
 */
@DisplayName("ServerConfigurationState reconciliation and serialization")
class ServerConfigurationStateTest {

    private static final UUID ID_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ID_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    private static JsonObject config(String name, UUID id) {
        JsonObject c = new JsonObject();
        c.addProperty(ServerConfigurationState.PLAYER_NAME, name);
        c.addProperty(ServerConfigurationState.PLAYER_ID, id.toString());
        return c;
    }

    @Test
    @DisplayName("put indexes a config by both id and name")
    void putIndexesByIdAndName() {
        ServerConfigurationState state = new ServerConfigurationState();
        JsonObject config = config("Zannagh", ID_A);
        state.put(ID_A, config);

        assertSame(config, state.getPlayerConfigs().get(ID_A));
        assertSame(config, state.getPlayerNameConfigs().get("Zannagh"));
    }

    @Test
    @DisplayName("put with no playerName still stores by id but adds no name index")
    void putWithoutNameSkipsNameIndex() {
        ServerConfigurationState state = new ServerConfigurationState();
        JsonObject nameless = new JsonObject();
        nameless.addProperty(ServerConfigurationState.PLAYER_ID, ID_A.toString());
        state.put(ID_A, nameless);

        assertSame(nameless, state.getPlayerConfigs().get(ID_A));
        assertTrue(state.getPlayerNameConfigs().isEmpty());
    }

    @Test
    @DisplayName("a name collision re-points the by-name index at the newest config")
    void nameCollisionRepointsByNameIndex() {
        ServerConfigurationState state = new ServerConfigurationState();
        // Same display name, two different accounts (a rename/nick scenario).
        state.put(ID_A, config("Steve", ID_A));
        JsonObject newer = config("Steve", ID_B);
        state.put(ID_B, newer);

        assertSame(newer, state.getPlayerNameConfigs().get("Steve"),
                "the by-name lookup must resolve to the most recently stored config");
        assertEquals(2, state.getPlayerConfigs().size());
    }

    @Test
    @DisplayName("toJson emits serverWideSettings, playerConfigs, and playerNameConfigs")
    void toJsonEmitsAllSections() {
        ServerConfigurationState state = new ServerConfigurationState();
        state.put(ID_A, config("Zannagh", ID_A));

        JsonObject json = state.toJson();

        assertTrue(json.has("serverWideSettings"));
        assertTrue(json.getAsJsonObject("playerConfigs").has(ID_A.toString()));
        assertTrue(json.getAsJsonObject("playerNameConfigs").has("Zannagh"),
                "playerNameConfigs must be emitted - the client never rebuilds it");
    }

    @Test
    @DisplayName("toJson deep-copies serverWideSettings so later mutation cannot leak into the document")
    void toJsonDeepCopiesSettings() {
        ServerConfigurationState state = new ServerConfigurationState();
        JsonObject json = state.toJson();
        json.getAsJsonObject("serverWideSettings")
                .addProperty(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION, false);

        assertTrue(ServerWideSettingsDefaults.readBoolean(state.getServerWideSettings(),
                ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION),
                "mutating the emitted copy must not affect live state");
    }

    @Test
    @DisplayName("setServerWideSettings routes through fillMissing")
    void setServerWideSettingsFillsMissing() {
        ServerConfigurationState state = new ServerConfigurationState();
        JsonObject partial = new JsonObject();
        partial.addProperty(ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF, true);
        state.setServerWideSettings(partial);

        JsonObject live = state.getServerWideSettings();
        assertTrue(live.has(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION), "missing keys must be filled in");
        assertTrue(ServerWideSettingsDefaults.readBoolean(live, ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF));
    }

    @Test
    @DisplayName("readPlayerName returns null for absent, non-primitive, or null configs")
    void readPlayerNameNullSafety() {
        assertNull(ServerConfigurationState.readPlayerName(null));
        assertNull(ServerConfigurationState.readPlayerName(new JsonObject()));
        JsonObject arrayValued = new JsonObject();
        arrayValued.add(ServerConfigurationState.PLAYER_NAME, new JsonArray());
        assertNull(ServerConfigurationState.readPlayerName(arrayValued));
        assertEquals("Zannagh", ServerConfigurationState.readPlayerName(config("Zannagh", ID_A)));
    }

    @Test
    @DisplayName("readPlayerId returns null for absent, malformed, or non-primitive UUIDs")
    void readPlayerIdNullSafety() {
        assertNull(ServerConfigurationState.readPlayerId(null));
        assertNull(ServerConfigurationState.readPlayerId(new JsonObject()));

        JsonObject badUuid = new JsonObject();
        badUuid.addProperty(ServerConfigurationState.PLAYER_ID, "definitely-not-a-uuid");
        assertNull(ServerConfigurationState.readPlayerId(badUuid));

        assertEquals(ID_A, ServerConfigurationState.readPlayerId(config("Zannagh", ID_A)));
    }

    @Test
    @DisplayName("a freshly constructed state has default server-wide settings and no players")
    void freshStateDefaults() {
        ServerConfigurationState state = new ServerConfigurationState();
        assertTrue(state.getPlayerConfigs().isEmpty());
        assertTrue(state.getPlayerNameConfigs().isEmpty());
        assertFalse(state.getServerWideSettings().entrySet().isEmpty());
    }
}
