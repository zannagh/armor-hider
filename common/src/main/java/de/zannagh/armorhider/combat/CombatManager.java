package de.zannagh.armorhider.combat;

import de.zannagh.armorhider.log.DebugLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class CombatManager {

    private static final double fadePer25Ms = 0.005;
    private static final Map<String, Long> combatTimes = new HashMap<>();

    private static void clearCombatTimesOlderThanTenSeconds() {
        var removalKeys = new ArrayList<String>();
        combatTimes.forEach((k, v) -> {
            if (System.currentTimeMillis() - v > 10000f) {
                removalKeys.add(k);
            }
        });
        removalKeys.forEach(combatTimes::remove);
    }

    public static void logCombat(String playerDisplayName) {
        clearCombatTimesOlderThanTenSeconds();
        if (!playerDisplayName.isEmpty()) {
            combatTimes.put(playerDisplayName, System.currentTimeMillis());
        }
    }

    public static void logCombat(String playerDisplayName, long timestamp) {
        clearCombatTimesOlderThanTenSeconds();
        if (!playerDisplayName.isEmpty()) {
            combatTimes.put(playerDisplayName, timestamp);
        }
    }

    public static double transformTransparencyBasedOnCombat(String playerDisplayName, double transparency) {
        clearCombatTimesOlderThanTenSeconds();
        if (playerDisplayName.isEmpty()) {
            DebugLogger.log("Combat logger queried for transparency but player display name was empty.");
            return transparency;
        }
        if (combatTimes.containsKey(playerDisplayName)) {
            var lastCombatTime = combatTimes.get(playerDisplayName);
            var milliSecondDiff = System.currentTimeMillis() - lastCombatTime;
            var steps = milliSecondDiff / 25;
            var fade = steps * fadePer25Ms;

            double result = 1 - fade;
            if (result < transparency) {
                result = transparency;
            }
            if (result >= 1) {
                result = 1;
            }
            DebugLogger.log("Combat logger finalized evaluation, returning transparency: " + result);
            return result;
        }
        return transparency;
    }
}
