package de.zannagh.armorhider.server;

import de.zannagh.armorhider.StringServerConfigProvider;
import de.zannagh.armorhider.configuration.ConfigurationItemFactoryRegistry;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The server-side config store: the admin toggles, the name-collision reconciliation in {@link
 * ServerConfigStore#put} (the mod-side twin of the plugin's {@code ServerConfigurationState.put}),
 * and the pass-through of the {@code ConfigurationProvider} contract. Driven by the in-memory
 * {@link StringServerConfigProvider} test double so no filesystem is touched.
 */
@DisplayName("ServerConfigStore toggles, reconciliation and delegation")
class ServerConfigStoreTest {

    @BeforeAll
    static void initializeFactories() {
        ConfigurationItemFactoryRegistry.initialize();
    }

    private static ServerConfigStore emptyStore() {
        return new ServerConfigStore(new StringServerConfigProvider(""));
    }

    @Test
    @DisplayName("setServerCombatDetection writes through to server-wide settings")
    void setsCombatDetection() {
        ServerConfigStore store = emptyStore();
        store.setServerCombatDetection(false);
        assertFalse(store.getConfig().serverWideSettings.enableCombatDetection.getValue());
        store.setServerCombatDetection(true);
        assertTrue(store.getConfig().serverWideSettings.enableCombatDetection.getValue());
    }

    @Test
    @DisplayName("setGlobalOverride writes through to forceArmorHiderOff")
    void setsGlobalOverride() {
        ServerConfigStore store = emptyStore();
        store.setGlobalOverride(true);
        assertTrue(store.getConfig().serverWideSettings.forceArmorHiderOff.getValue());
    }

    @Test
    @DisplayName("put indexes a config by uuid and by name")
    void putIndexesByIdAndName() {
        ServerConfigStore store = emptyStore();
        UUID id = UUID.randomUUID();
        PlayerConfig config = new PlayerConfig(id, "Zannagh");

        store.put(id, config);

        assertSame(config, store.getConfig().playerConfigs.get(id));
        assertSame(config, store.getConfig().playerNameConfigs.get("Zannagh"));
    }

    @Test
    @DisplayName("a name collision re-points the by-name index at the newest config")
    void nameCollisionRepointsByName() {
        ServerConfigStore store = emptyStore();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        store.put(first, new PlayerConfig(first, "Steve"));
        PlayerConfig newer = new PlayerConfig(second, "Steve");

        store.put(second, newer);

        assertSame(newer, store.getConfig().playerNameConfigs.get("Steve"),
                "the most recently stored config must own the shared name");
        assertEquals(2, store.getConfig().playerConfigs.size());
    }

    @Test
    @DisplayName("the ConfigurationProvider contract is delegated to the backing provider")
    void delegatesProviderContract() {
        StringServerConfigProvider backing = new StringServerConfigProvider("");
        ServerConfigStore store = new ServerConfigStore(backing);

        assertSame(backing.getValue(), store.getValue(), "getValue must pass through");
        assertSame(store.getValue(), store.getConfig(), "getConfig mirrors getValue");

        ServerConfiguration replacement = new ServerConfiguration();
        store.setValue(replacement);
        assertSame(replacement, store.getValue());
        assertSame(replacement, backing.getValue(), "setValue must reach the backing provider");

        // getDefault always yields a fresh default instance, never the live value.
        ServerConfiguration def = store.getDefault();
        assertFalse(def == replacement);
    }
}
