package de.zannagh.armorhider.resources;

import de.zannagh.armorhider.common.CombatManager;
import de.zannagh.armorhider.configuration.items.implementations.ArmorOpacity;
import net.minecraft.entity.EquipmentSlot;

public record ArmorModificationInfo(EquipmentSlot equipmentSlot, PlayerConfig playerConfig) {

    
    public double GetTransparency() {
        var setting = switch (equipmentSlot) {
            case HEAD -> playerConfig.helmetOpacity.getValue();
            case CHEST -> playerConfig.chestOpacity.getValue();
            case LEGS -> playerConfig.legsOpacity.getValue();
            case FEET -> playerConfig.bootsOpacity.getValue();
            default -> 1.0;
        };
        return CombatManager.transformTransparencyBasedOnCombat(playerConfig.playerName.getValue(), setting);
    }

    public boolean ShouldHide() {
        double transparency = GetTransparency();
        return transparency < ArmorOpacity.TRANSPARENCY_STEP + ArmorOpacity.TRANSPARENCY_STEP / 2;
    }

    public boolean ShouldModify() {
        double transparency = GetTransparency();
        return transparency < 1 - ArmorOpacity.TRANSPARENCY_STEP / 2;
    }
}
