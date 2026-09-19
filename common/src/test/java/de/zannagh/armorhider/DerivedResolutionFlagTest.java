package de.zannagh.armorhider;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.zannagh.armorhider.configuration.ConfigurationItemFactoryRegistry;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code derivedResolution} marker says "this PlayerConfig was synthesized per call to answer
 * 'how do I render that stranger', it is not a config anybody owns". It is process-local by design:
 * it must never reach the config JSON on disk, the network payload, or any config derived from a
 * tagged one. These tests pin that contract.
 */
class DerivedResolutionFlagTest {

    @BeforeAll
    static void initializeFactories() {
        ConfigurationItemFactoryRegistry.initialize();
    }

    private static PlayerConfig taggedConfig() {
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Stranger");
        config.markAsDerivedResolution();
        return config;
    }

    /**
     * Walks every nested object of the serialized config, so a leak through the embedded
     * global/individual overrides is caught too.
     */
    private static boolean jsonTreeContainsKey(JsonElement root, String key) {
        Deque<JsonElement> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            JsonElement current = pending.pop();
            if (current.isJsonObject()) {
                JsonObject object = current.getAsJsonObject();
                if (object.has(key)) {
                    return true;
                }
                for (var entry : object.entrySet()) {
                    pending.push(entry.getValue());
                }
            } else if (current.isJsonArray()) {
                for (JsonElement element : current.getAsJsonArray()) {
                    pending.push(element);
                }
            }
        }
        return false;
    }

    @Test
    @DisplayName("A freshly constructed config is not a derived resolution")
    void defaultsToFalse() {
        assertFalse(new PlayerConfig().isDerivedResolution(),
                "the no-arg (GSON) constructor must produce an owned config, not a derived resolution");
        assertFalse(new PlayerConfig(UUID.randomUUID(), "Owner").isDerivedResolution(),
                "the (uuid, name) constructor must produce an owned config, not a derived resolution");
        assertFalse(PlayerConfig.defaults(UUID.randomUUID(), "Owner").isDerivedResolution(),
                "defaults() must produce an owned config, not a derived resolution");
    }

    @Test
    @DisplayName("markAsDerivedResolution sets the flag and returns the same instance")
    void markerIsFluent() {
        var config = PlayerConfig.defaults(UUID.randomUUID(), "Stranger");

        var returned = config.markAsDerivedResolution();

        assertSame(config, returned, "the marker must be fluent so call sites can return it inline");
        assertTrue(config.isDerivedResolution(), "the marker must actually set the flag");
    }

    @Test
    @DisplayName("The derived-resolution marker is not written to the config JSON")
    void markerDoesNotSerialize() {
        var config = taggedConfig();
        config.useGlobalOverrideForAllPlayers.setValue(true);
        config.globalPlayerOverride = taggedConfig();
        config.individualConfigurations.putOverride("mc.example.com", "TargetPlayer", taggedConfig());

        JsonElement json = JsonParser.parseString(config.toJson());

        assertFalse(jsonTreeContainsKey(json, "derivedResolution"),
                "the process-local marker must never reach the config JSON, at any nesting level");
    }

    @Test
    @DisplayName("forNetwork() of a derived resolution is not itself derived")
    void markerDoesNotSurviveForNetwork() {
        var network = taggedConfig().forNetwork();

        assertFalse(network.isDerivedResolution(),
                "a config built for the wire must never carry the client-local marker");
        assertFalse(jsonTreeContainsKey(JsonParser.parseString(network.toJson()), "derivedResolution"),
                "the encoded network payload must not contain the marker either");
    }

    @Test
    @DisplayName("deepCopy() of a derived resolution is not itself derived")
    void markerDoesNotSurviveDeepCopy() {
        var original = taggedConfig();

        var copy = original.deepCopy("Copy", UUID.randomUUID());

        assertTrue(original.isDerivedResolution(), "precondition: the source must be tagged");
        assertFalse(copy.isDerivedResolution(),
                "tagging happens at the call site, so a deep copy must start out untagged");
    }

    @Test
    @DisplayName("migrate() of a derived resolution is not itself derived")
    void markerDoesNotSurviveMigrate() {
        var migrated = PlayerConfig.migrate(taggedConfig());

        assertFalse(migrated.isDerivedResolution(),
                "migration rebuilds the config through a constructor, so the marker must not carry over");
    }

    @Test
    @DisplayName("A derived resolution loses its marker across a serialize/deserialize round-trip")
    void markerDoesNotSurviveRoundTrip() {
        var config = taggedConfig();
        config.helmetOpacity.setValue(0.42);

        var restored = PlayerConfig.deserialize(config.toJson());

        assertFalse(restored.isDerivedResolution(),
                "a config loaded from disk is owned by the user and must never look like a derived resolution");
    }
}
