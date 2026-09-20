package de.zannagh.armorhider.combat;

import de.zannagh.armorhider.api.combat.ArmorHiderCombatEvent;
import de.zannagh.armorhider.api.combat.ArmorHiderCombatEventConsumer;
import de.zannagh.armorhider.api.combat.ArmorHiderCombatManagementApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The default combat state machine, exercised through a fresh {@link CombatManager} instance so no
 * global API singleton is touched. Covers the in-combat window, the "snap opaque then fade back to
 * the configured transparency" curve, the empty-name guards, event expiry, and custom-manager
 * override delegation - which together drive {@link CombatEventConsumerImpl}, {@link DefaultCombatEvent}
 * and {@code InjectorFactory}.
 */
@DisplayName("CombatManager combat window and fade curve")
class CombatManagerTest {

    private static final String PLAYER = "Zannagh";
    private static final double CONFIGURED_TRANSPARENCY = 0.3;

    @Test
    @DisplayName("a player with no combat event is not in combat and keeps their configured transparency")
    void noCombatKeepsTransparency() {
        CombatManager manager = new CombatManager();
        assertFalse(manager.isInCombat(PLAYER));
        assertEquals(CONFIGURED_TRANSPARENCY, manager.getCombatFade(PLAYER, CONFIGURED_TRANSPARENCY));
    }

    @Test
    @DisplayName("a fresh combat event puts the player in combat and snaps armor fully opaque")
    void freshCombatSnapsOpaque() {
        CombatManager manager = new CombatManager();
        manager.registerCombatEvent(PLAYER, System.currentTimeMillis());

        assertTrue(manager.isInCombat(PLAYER));
        // progress ~0 -> result = 1.0 - 0*(1-0.3) = 1.0 (fully opaque, armor forced visible).
        assertEquals(1.0, manager.getCombatFade(PLAYER, CONFIGURED_TRANSPARENCY), 0.05);
    }

    @Test
    @DisplayName("mid-window the fade sits between opaque and the configured transparency")
    void midWindowFadesBack() {
        CombatManager manager = new CombatManager();
        // 5 s into the default 10 s fade -> progress ~0.5 -> result ~= 1 - 0.5*(1-0.3) = 0.65.
        manager.registerCombatEvent(PLAYER, System.currentTimeMillis() - 5_000L);

        double fade = manager.getCombatFade(PLAYER, CONFIGURED_TRANSPARENCY);
        assertTrue(fade > CONFIGURED_TRANSPARENCY && fade < 1.0,
                () -> "mid-window fade should be between configured and opaque, was " + fade);
        assertEquals(0.65, fade, 0.1);
    }

    @Test
    @DisplayName("an expired event is swept and the player returns to their configured transparency")
    void expiredEventIsSwept() {
        CombatManager manager = new CombatManager();
        // 20 s ago, past the 10 s fade window.
        manager.registerCombatEvent(PLAYER, System.currentTimeMillis() - 20_000L);

        // getCombatFade sweeps expired events, so it must report the configured transparency again...
        assertEquals(CONFIGURED_TRANSPARENCY, manager.getCombatFade(PLAYER, CONFIGURED_TRANSPARENCY));
        // ...and the sweep clears the in-combat flag too.
        assertFalse(manager.isInCombat(PLAYER));
    }

    @Test
    @DisplayName("an empty player name never registers combat and is never in combat")
    void emptyNameIgnored() {
        CombatManager manager = new CombatManager();
        manager.registerCombatEvent("");
        manager.registerCombatEvent("", System.currentTimeMillis());

        assertFalse(manager.isInCombat(""));
        assertEquals(CONFIGURED_TRANSPARENCY, manager.getCombatFade("", CONFIGURED_TRANSPARENCY));
    }

    @Test
    @DisplayName("registering via the event overload also puts the player in combat")
    void registerViaEventOverload() {
        CombatManager manager = new CombatManager();
        manager.registerCombatEvent(new DefaultCombatEvent(PLAYER, System.currentTimeMillis()));
        assertTrue(manager.isInCombat(PLAYER));
    }

    @Test
    @DisplayName("a custom manager override takes over fade resolution")
    void customManagerOverridesFade() {
        CombatManager manager = new CombatManager();
        RecordingManager custom = new RecordingManager();
        manager.overrideDefaultBehavior(custom, 0);

        double fade = manager.getCombatFade(PLAYER, CONFIGURED_TRANSPARENCY);

        assertEquals(RecordingManager.FIXED_FADE, fade, "the override must own fade resolution");
        assertTrue(custom.fadeQueried, "the override should have been consulted");

        manager.registerCombatEvent(PLAYER);
        assertTrue(custom.registered, "registration should route to the override");
    }

    /** Minimal override that records the delegated calls and returns a distinctive fade. */
    private static final class RecordingManager implements ArmorHiderCombatManagementApi {
        static final double FIXED_FADE = 0.123;
        boolean registered;
        boolean fadeQueried;

        @Override
        public void overrideDefaultBehavior(ArmorHiderCombatManagementApi customManagement, int priority) {
        }

        @Override
        public void registerCombatEventConsumer(ArmorHiderCombatEventConsumer consumer) {
        }

        @Override
        public void registerCombatEvent(String playerDisplayName) {
            registered = true;
        }

        @Override
        public void registerCombatEvent(String playerDisplayName, long timestamp) {
            registered = true;
        }

        @Override
        public void registerCombatEvent(ArmorHiderCombatEvent event) {
            registered = true;
        }

        @Override
        public boolean isInCombat(String playerDisplayName) {
            return true;
        }

        @Override
        public double getCombatFade(String playerDisplayName, double originalTransparency) {
            fadeQueried = true;
            return FIXED_FADE;
        }
    }
}
