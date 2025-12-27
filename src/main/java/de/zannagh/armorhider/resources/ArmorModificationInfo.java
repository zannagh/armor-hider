package de.zannagh.armorhider.resources;

import de.zannagh.armorhider.common.CombatManager;
import net.minecraft.entity.EquipmentSlot;

public record ArmorModificationInfo(EquipmentSlot equipmentSlot, PlayerConfig playerConfig) {

    public static final double TransparencyStep = 0.05;
    public double GetTransparency() {
        var setting = switch (equipmentSlot) {
            case HEAD -> playerConfig.helmetTransparency;
            case CHEST -> playerConfig.chestTransparency;
            case LEGS -> playerConfig.legsTransparency;
            case FEET -> playerConfig.bootsTransparency;
            default -> 1.0;
        };
        return CombatManager.transformTransparencyBasedOnCombat(playerConfig.playerName, setting);
    }

    public boolean ShouldHide() {
        double transparency = GetTransparency();
        return transparency < TransparencyStep + TransparencyStep / 2;
    }

    public boolean ShouldModify() {
        double transparency = GetTransparency();
        return transparency < 1 - TransparencyStep / 2;
    }
}
