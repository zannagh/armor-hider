package de.zannagh.armorhider.client.net;

import de.zannagh.armorhider.configuration.ConfigurationItemFactoryRegistry;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SingleplayerServerSync decision logic")
class SingleplayerServerSyncTest {

    @BeforeAll
    static void initializeFactories() {
        ConfigurationItemFactoryRegistry.initialize();
    }

    private static PlayerConfig local(boolean combat, boolean invisibility, boolean disabled) {
        var config = new PlayerConfig(UUID.randomUUID(), "Tester");
        config.enableCombatDetection.setValue(combat);
        config.disableArmorHiderOnInvisibility.setValue(invisibility);
        config.disableArmorHider.setValue(disabled);
        return config;
    }

    @Test
    @DisplayName("no update when the mapped server values already match")
    void noUpdateWhenMatching() {
        var server = new ServerWideSettings(true, false, false, true);
        assertTrue(SingleplayerServerSync.computeUpdate(local(true, false, false), server).isEmpty());
    }

    @Test
    @DisplayName("disabling client combat detection turns the server value off")
    void combatDetectionFollowsClient() {
        var server = new ServerWideSettings(true, false, false, true);
        var update = SingleplayerServerSync.computeUpdate(local(false, false, false), server).orElseThrow();
        assertFalse(update.enableCombatDetection.getValue());
    }

    @Test
    @DisplayName("invisibility setting follows the client")
    void invisibilityFollowsClient() {
        var server = new ServerWideSettings(true, false, false, true);
        var update = SingleplayerServerSync.computeUpdate(local(true, true, false), server).orElseThrow();
        assertTrue(update.disableArmorHiderOnInvisibilityGlobally.getValue());
    }

    @Test
    @DisplayName("force-off and allow-individual are never synced and carry over from the server")
    void unmappedValuesCarryOver() {
        var server = new ServerWideSettings(true, true, false, false);
        // the client's own disable flag must not reach forceArmorHiderOff
        var update = SingleplayerServerSync.computeUpdate(local(false, false, true), server).orElseThrow();
        assertTrue(update.forceArmorHiderOff.getValue());
        assertFalse(update.allowIndividualPlayerConfigurations.getValue());
        assertEquals(true, SingleplayerServerSync.computeUpdate(local(true, false, true), new ServerWideSettings(true, false, false, true)).isEmpty());
    }
}
