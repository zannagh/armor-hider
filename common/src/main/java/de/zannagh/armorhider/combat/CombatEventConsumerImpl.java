package de.zannagh.armorhider.combat;

import de.zannagh.armorhider.api.combat.ArmorHiderCombatEvent;
import de.zannagh.armorhider.api.combat.ArmorHiderCombatEventConsumer;
import de.zannagh.armorhider.log.DebugLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CombatEventConsumerImpl implements ArmorHiderCombatEventConsumer {

    private final Map<String, ArmorHiderCombatEvent> combatEvents = new HashMap<>();

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

    @Override
    public boolean shouldHandle(ArmorHiderCombatEvent armorHiderCombatEvent) {
        return true;
    }

    @Override
    public boolean shouldHandleExclusively(ArmorHiderCombatEvent armorHiderCombatEvent) {
        return false;
    }

    @Override
    public ArmorHiderCombatEvent handle(ArmorHiderCombatEvent armorHiderCombatEvent) {
        clearOldCombatEvents();
        combatEvents.put(armorHiderCombatEvent.getPlayerDisplayName(), armorHiderCombatEvent);
        return armorHiderCombatEvent;
    }

    @Override
    public double getFadeFor(String playerDisplayName, double originalTransparency) {
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

    @Override
    public boolean isPlayerConsideredInCombat(String playerDisplayName) {
        return combatEvents.containsKey(playerDisplayName);
    }
}
