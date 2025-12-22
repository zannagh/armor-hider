package de.zannagh.armorhider.resources;

import de.zannagh.armorhider.client.CombatManager;
import net.minecraft.entity.EquipmentSlot;

public class ArmorModificationInfo {
    private final PlayerConfig playerConfig;
    private final String playerName;
    private final EquipmentSlot equipmentSlot;
    public ArmorModificationInfo(EquipmentSlot slot, PlayerConfig config) {
        equipmentSlot = slot;
        playerConfig = config;
        playerName = config.playerName;
    }
    
    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }
    
    public double GetTransparency(){
        var setting = switch (equipmentSlot) {
            case HEAD -> playerConfig.helmetTransparency;
            case CHEST -> playerConfig.chestTransparency;
            case LEGS -> playerConfig.legsTransparency;
            case FEET -> playerConfig.bootsTransparency;
            default -> 1.0;
        };
        return CombatManager.transformTransparencyBasedOnCombat(playerName, setting);
    }
    
    public boolean ShouldHide() {
        double transparency = GetTransparency();
        return transparency < 0.1;
    }
    
    public boolean ShouldModify(){
        double transparency = GetTransparency();
        return transparency < 0.995;
    }
    
     public String GetSlotName(){
        return switch (equipmentSlot) {
            case HEAD -> "head";
            case CHEST -> "chest";
            case LEGS -> "legs";
            case FEET -> "feet";
            default -> "none";
        };
     }
}
