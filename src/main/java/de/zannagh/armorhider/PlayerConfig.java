
package de.zannagh.armorhider;

import java.util.UUID;

public class PlayerConfig {
    
    public static PlayerConfig FromPacket(double helmet, double chest, double legs, double boots, String uuid){
        return new PlayerConfig(helmet, chest, legs, boots, uuid);
    }
    public PlayerConfig(double helmet, double chest, double legs, double boots, String uuid){
        helmetTransparency = helmet;
        chestTransparency = chest;
        legsTransparency = legs;
        bootsTransparency = boots;
        playerId = UUID.fromString(uuid);
    }
    public PlayerConfig(UUID uuid) {
        playerId = uuid;
    }
    public double helmetTransparency = 0.0;
    public double chestTransparency = 0.0;
    public double legsTransparency = 1.0;
    public double bootsTransparency = 0.0;
    public UUID playerId;
    public static PlayerConfig defaults(UUID playerId) { return new PlayerConfig(playerId); }
}