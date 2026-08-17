package de.zannagh.armorhider.server;

import de.zannagh.armorhider.configuration.ConfigurationItemFactoryRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Disk persistence for the mod-side server config. Exercised against a temp-dir path, covering the
 * missing-file bootstrap, the save/reload round-trip, and the corrupt-file recovery that flags the
 * config for a rewrite rather than propagating an exception.
 */
@DisplayName("ServerConfigFileProvider disk persistence")
class ServerConfigFileProviderTest {

    @BeforeAll
    static void initializeFactories() {
        ConfigurationItemFactoryRegistry.initialize();
    }

    @Test
    @DisplayName("constructing against a missing file writes a fresh default and loads it")
    void missingFileBootstrapsDefault(@TempDir Path dir) {
        Path file = dir.resolve("nested").resolve("armor-hider-server.json");
        ServerConfigFileProvider provider = new ServerConfigFileProvider(file);

        assertNotNull(provider.getValue());
        assertTrue(Files.exists(file), "a missing config must be created on first load");
    }

    @Test
    @DisplayName("a saved value round-trips through a fresh provider over the same file")
    void savedValueRoundTrips(@TempDir Path dir) {
        Path file = dir.resolve("armor-hider-server.json");
        ServerConfigFileProvider provider = new ServerConfigFileProvider(file);
        provider.getValue().serverWideSettings.forceArmorHiderOff.setValue(true);
        provider.saveCurrent();

        ServerConfigFileProvider reopened = new ServerConfigFileProvider(file);
        assertTrue(reopened.getValue().serverWideSettings.forceArmorHiderOff.getValue(),
                "a persisted setting must survive a reload");
    }

    @Test
    @DisplayName("a corrupt config file recovers to a default instead of throwing")
    void corruptFileRecovers(@TempDir Path dir) throws IOException {
        // A hand-edited/truncated file makes ServerConfiguration.deserialize throw the unchecked
        // JsonSyntaxException; load() must catch it and fall back rather than crash the constructor
        // (parity with the paper-side ServerConfigStorage.load()).
        Path file = dir.resolve("armor-hider-server.json");
        Files.writeString(file, "}{ not valid json");

        ServerConfigFileProvider provider = new ServerConfigFileProvider(file);
        assertNotNull(provider.getValue(), "load must fall back to a usable default on corrupt input");
    }

    @Test
    @DisplayName("getDefault yields a fresh instance distinct from the live value")
    void getDefaultIsFresh(@TempDir Path dir) {
        ServerConfigFileProvider provider = new ServerConfigFileProvider(dir.resolve("armor-hider-server.json"));
        ServerConfiguration def = provider.getDefault();
        assertNotNull(def);
        assertFalse(def == provider.getValue(), "getDefault must not hand back the live config");
    }

    @Test
    @DisplayName("save replaces the live value and persists it")
    void saveReplacesAndPersists(@TempDir Path dir) {
        Path file = dir.resolve("armor-hider-server.json");
        ServerConfigFileProvider provider = new ServerConfigFileProvider(file);

        ServerConfiguration replacement = new ServerConfiguration();
        replacement.serverWideSettings.enableCombatDetection.setValue(false);
        provider.save(replacement);

        assertFalse(provider.getValue().serverWideSettings.enableCombatDetection.getValue());
        assertTrue(Files.exists(file));
    }
}
