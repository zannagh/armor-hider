package de.zannagh.armorhider.resources;

import de.zannagh.armorhider.combat.CombatManager;
import de.zannagh.armorhider.configuration.items.ArmorOpacity;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public record ArmorModificationInfo(EquipmentSlot equipmentSlot, @NotNull PlayerConfig playerConfig) {
    public double getTransparency() {
        var setting = switch (equipmentSlot) {
            case HEAD -> playerConfig.helmetOpacity.getValue();
            case CHEST -> playerConfig.chestOpacity.getValue();
            case LEGS -> playerConfig.legsOpacity.getValue();
            case FEET -> playerConfig.bootsOpacity.getValue();
            case OFFHAND -> playerConfig.offHandOpacity.getValue();
            default -> 1.0;
        };
        return CombatManager.transformTransparencyBasedOnCombat(playerConfig.playerName.getValue(), setting);
    }
    
    public boolean shouldDisableGlint() {
        return switch (equipmentSlot) {
            case HEAD -> !playerConfig.helmetGlint.getValue();
            case CHEST -> !playerConfig.chestGlint.getValue();
            case LEGS -> !playerConfig.legsGlint.getValue();
            case FEET -> !playerConfig.bootsGlint.getValue();
            default -> false;
        };
    }

    public boolean shouldHide() {
        double transparency = getTransparency();
        return transparency < ArmorOpacity.TRANSPARENCY_STEP;
    }

    public boolean shouldModify() {
        double transparency = getTransparency();
        return (transparency < 1 - ArmorOpacity.TRANSPARENCY_STEP / 2) || shouldDisableGlint();
    }

    public boolean isConfigForRemotePlayer(String localPlayerName) {
        return !playerConfig.playerName.getValue().equals(localPlayerName);
    }
}
