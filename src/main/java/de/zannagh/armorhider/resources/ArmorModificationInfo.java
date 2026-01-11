package de.zannagh.armorhider.resources;

import de.zannagh.armorhider.common.CombatManager;
import de.zannagh.armorhider.configuration.items.implementations.ArmorOpacity;
import net.minecraft.entity.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public record ArmorModificationInfo(EquipmentSlot equipmentSlot, @NotNull PlayerConfig playerConfig) {
    
    public double getTransparency() {
        var setting = switch (equipmentSlot) {
            case HEAD -> playerConfig.helmetOpacity.getValue();
            case CHEST -> playerConfig.chestOpacity.getValue();
            case LEGS -> playerConfig.legsOpacity.getValue();
            case FEET -> playerConfig.bootsOpacity.getValue();
            default -> 1.0;
        };
        return CombatManager.transformTransparencyBasedOnCombat(playerConfig.playerName.getValue(), setting);
    }

    public boolean shouldHide() {
        double transparency = getTransparency();
        return transparency < ArmorOpacity.TRANSPARENCY_STEP + ArmorOpacity.TRANSPARENCY_STEP / 2;
    }

    public boolean shouldModify() {
        double transparency = getTransparency();
        return transparency < 1 - ArmorOpacity.TRANSPARENCY_STEP / 2;
    }
    
    public boolean isConfigForRemotePlayer(String localPlayerName) {
        return !playerConfig.playerName.getValue().equals(localPlayerName);
    }
}
