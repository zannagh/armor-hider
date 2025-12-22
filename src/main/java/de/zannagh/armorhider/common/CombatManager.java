package de.zannagh.armorhider.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class CombatManager {
    
    private static final double fadePer25Ms = 0.005;
    private static final Map<String, Long> combatTimes = new HashMap<>();
    
    private static void clearCombatTimesOlderThanTenSeconds(){
        var removalKeys = new ArrayList<String>();
        combatTimes.forEach((k, v ) ->{
            if (System.currentTimeMillis() - v > 10000f) {
                removalKeys.add(k);
            }
        });
        removalKeys.forEach(combatTimes::remove);
    }
    
    public static void logCombat(String player){
        clearCombatTimesOlderThanTenSeconds();
        combatTimes.put(player, System.currentTimeMillis());
    }
    
    public static double transformTransparencyBasedOnCombat(String player, double transparency){
        clearCombatTimesOlderThanTenSeconds();
        if (combatTimes.containsKey(player)){
            var lastCombatTime = combatTimes.get(player);
            
            var milliSecondDiff =  System.currentTimeMillis() - lastCombatTime;
            var steps = milliSecondDiff / 25;
            var fade = steps * fadePer25Ms;
            
            double result = 1 - fade;
            if (result < transparency){
                return transparency;
            }
            if (result >= 1) {
                return 1;
            }
            return result;
        }
        return transparency;
    }
}
