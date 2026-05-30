package de.zannagh.armorhider.combat;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.api.combat.ArmorHiderCombatEvent;
import de.zannagh.armorhider.api.combat.ArmorHiderCombatEventConsumer;
import de.zannagh.armorhider.api.combat.ArmorHiderCombatManagement;
import de.zannagh.armorhider.log.DebugLogger;

import java.util.*;

/**
 * A default implementation of {@link ArmorHiderCombatManagement}.
 */
public class CombatManager implements ArmorHiderCombatManagement {

    private final Map<String, ArmorHiderCombatEvent> combatEvents = new HashMap<>();

    private final ArrayList<ArmorHiderCombatEventConsumer> customConsumers = new ArrayList<>();

    private final ArrayList<Pair<ArmorHiderCombatManagement, Integer>> customManagers = new ArrayList<>();

    // Static delegates for backwards compatibility with existing call sites.
    public static void logCombat(String playerDisplayName) {
        ArmorHiderApi.getInstance().getCombatManagement().registerCombatEvent(new DefaultCombatEvent(playerDisplayName, System.currentTimeMillis()));
    }

    public static void logCombat(String playerDisplayName, long timestamp) {
        ArmorHiderApi.getInstance().getCombatManagement().registerCombatEvent(new DefaultCombatEvent(playerDisplayName, timestamp));
    }

    public static boolean isPlayerInCombat(String playerDisplayName) {
        return ArmorHiderApi.getInstance().getCombatManagement().isInCombat(playerDisplayName);
    }

    public static double transformTransparencyBasedOnCombat(String playerDisplayName, double transparency) {
        return ArmorHiderApi.getInstance().getCombatManagement().getCombatFade(playerDisplayName, transparency);
    }

    private void clearOldCombatEvents() {
        var removalKeys = new ArrayList<String>();
        combatEvents.forEach((k, v) -> {
            var maxAge = v.getTimestamp() + v.getFadeDurationSeconds() * 1000;
            if (maxAge < System.currentTimeMillis()) {
                removalKeys.add(k);
            }
        });
        removalKeys.forEach(combatEvents::remove);
    }

    public void registerCombatEvent(String playerDisplayName) {
        if (!customManagers.isEmpty()) {
            customManagers.sort(Comparator.comparingInt(Pair::getSecond));
            customManagers.get(0).getFirst().registerCombatEvent(new DefaultCombatEvent(playerDisplayName, System.currentTimeMillis()));
            return;
        }
        clearOldCombatEvents();
        if (!playerDisplayName.isEmpty()) {
            registerCombatEvent(new DefaultCombatEvent(playerDisplayName, System.currentTimeMillis()));
        }
    }

    public void registerCombatEvent(String playerDisplayName, long timestamp) {
        if (!customManagers.isEmpty()) {
            customManagers.sort(Comparator.comparingInt(Pair::getSecond));
            customManagers.get(0).getFirst().registerCombatEvent(new DefaultCombatEvent(playerDisplayName, timestamp));
            return;
        }
        clearOldCombatEvents();
        if (!playerDisplayName.isEmpty()) {
            registerCombatEvent(new DefaultCombatEvent(playerDisplayName, timestamp));
        }
    }

    @Override
    public void overrideDefaultBehavior(ArmorHiderCombatManagement customManagement, int priority) {
        customManagers.add(Pair.of(customManagement, priority));
    }

    @Override
    public void registerCombatEventConsumer(ArmorHiderCombatEventConsumer consumer) {
        customConsumers.add(consumer);
    }

    @Override
    public void registerCombatEvent(ArmorHiderCombatEvent event) {
        if (!customManagers.isEmpty()) {
            customManagers.sort(Comparator.comparingInt(Pair::getSecond));
            customManagers.get(0).getFirst().registerCombatEvent(event);
            return;
        }
        if (customConsumers.isEmpty()) {
            combatEvents.put(event.getPlayerDisplayName(), event);
            return;
        }
        customConsumers.sort(Comparator.comparingLong(ArmorHiderCombatEventConsumer::getPriority));
        for(ArmorHiderCombatEventConsumer consumer : customConsumers) {
            if (consumer.shouldConsumeExclusively(event)) {
                event = consumer.apply(event);
                combatEvents.put(event.getPlayerDisplayName(), event);
                return;
            }
            if (consumer.shouldConsume(event)) {
                event = consumer.apply(event);
                combatEvents.put(event.getPlayerDisplayName(), event);
            }
        };
    }

    /**
     @inheritDoc
     */
    public boolean isInCombat(String playerDisplayName) {
        clearOldCombatEvents();
        return !playerDisplayName.isEmpty() && combatEvents.containsKey(playerDisplayName);
    }

    public double getCombatFade(String playerDisplayName, double originalTransparency) {
        if (!customManagers.isEmpty()) {
            customManagers.sort(Comparator.comparingInt(Pair::getSecond));
            return customManagers.get(0).getFirst().getCombatFade(playerDisplayName, originalTransparency);
        }
        clearOldCombatEvents();
        if (playerDisplayName.isEmpty()) {
            DebugLogger.log("Combat logger queried for transparency but player display name was empty.");
            return originalTransparency;
        }
        if (combatEvents.containsKey(playerDisplayName)) {
            var lastEvent = combatEvents.get(playerDisplayName);
            var fadeDurationMs = lastEvent.getFadeDurationSeconds() * 1000;
            var elapsedCombatTime = System.currentTimeMillis() - lastEvent.getTimestamp();
            double progress = Math.min(1.0, (double) elapsedCombatTime / fadeDurationMs);

            double result = 1.0 - progress * (1.0 - originalTransparency);
            DebugLogger.log("Combat logger finalized evaluation, returning transparency: {}", result);
            return result;
        }
        return originalTransparency;
    }
}
