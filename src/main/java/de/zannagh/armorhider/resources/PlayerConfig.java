
package de.zannagh.armorhider.resources;

import de.zannagh.armorhider.ArmorHider;
import net.minecraft.util.Pair;

import java.io.Reader;
import java.sql.Ref;
import java.util.UUID;

public class PlayerConfig {
    public static PlayerConfig FromPacket(double helmet, double chest, double legs, double boots, boolean enableCombatDetection, String uuid, String playerName){
        return new PlayerConfig(helmet, chest, legs, boots, enableCombatDetection, uuid, playerName);
    }
    
    public PlayerConfig(double helmet, double chest, double legs, double boots, boolean combatDetection, String uuid, String name){
        helmetTransparency = helmet;
        chestTransparency = chest;
        legsTransparency = legs;
        bootsTransparency = boots;
        enableCombatDetection = combatDetection;
        playerId = UUID.fromString(uuid);
        playerName = name;
    }
    
    public PlayerConfig(UUID uuid, String name) {
        playerId = uuid;
        playerName = name;
    }
    
    public static Pair<PlayerConfig, Boolean> Deserialize(Reader reader){
        PlayerConfig c = ArmorHider.GSON.fromJson(reader, PlayerConfig.class);
        var hasChanged = c.setNullEntriesToDefault(c.playerId, c.playerName);
        return new Pair<>(c, hasChanged);
    }
    
    private Boolean setNullEntriesToDefault(UUID uuid, String name){
        Boolean hasChangedSettings = false;
        var defaults = defaults(uuid, name);
        if (playerId == null) {
            playerId = defaults.playerId;
        }
        if (playerName == null) {
            playerName = defaults.playerName;
        }
        if (enableCombatDetection == null) {
            enableCombatDetection = defaults.enableCombatDetection;
            hasChangedSettings = true;
        }
        return hasChangedSettings;
    }
    
    public double helmetTransparency = 1.0;
    public double chestTransparency = 1.0;
    public double legsTransparency = 1.0;
    public double bootsTransparency = 1.0;
    public UUID playerId;
    public String playerName;
    public Boolean enableCombatDetection = true;
    public static PlayerConfig defaults(UUID playerId, String playerName) { return new PlayerConfig(playerId, playerName); }
}