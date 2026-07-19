package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.CompatManager;
import de.zannagh.armorhider.api.CompatFlags;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.SlotModification;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EquipmentSlot;

public final class FantasyArmorCompat {

    public static void forceArmVisibility(Object entity, Object playerModel) {
        if (!CompatManager.requiresCompatTo(CompatFlags.FANTASY_ARMOR)) {
            return;
        }
        if (!(entity instanceof IdentityCarrier carrier)) {
            return;
        }

        String name = carrier.armorHider$playerName();
        if (name == null) {
            return;
        }

        if (SlotModification.isSlotModified(name, EquipmentSlot.CHEST, carrier.armorHider$getItemBySlot(EquipmentSlot.CHEST))) {
            //? if >= 1.21.2
            var model = ((LivingEntityRenderer<?, ?, ?>) (Object) playerModel).getModel();
            //? if < 1.21.2
            //var model = ((LivingEntityRenderer<?, ?>) (Object) playerModel).getModel();

            //? if >= 1.21.2 {
            if (model instanceof PlayerModel humanoid) {
                humanoid.leftArm.visible = true;
                humanoid.leftSleeve.visible = true;
                humanoid.rightArm.visible = true;
                humanoid.rightSleeve.visible = true;
            }
            //? } else {
            /*if (model instanceof HumanoidModel<?> humanoid) {
                humanoid.leftArm.visible = true;
                humanoid.rightArm.visible = true;
            }
            *///? }
        }
    }

    public static void resetArmVisibility(PlayerModel model, Object state) {
        if (!CompatManager.requiresCompatTo(CompatFlags.FANTASY_ARMOR)) {
            return;
        }
        if (!(state instanceof IdentityCarrier carrier)) {
            return;
        }
        var mod = SlotModification.of(carrier.armorHider$playerName(), EquipmentSlot.CHEST, carrier.armorHider$getItemBySlot(EquipmentSlot.CHEST));
        if (!mod.needsModification()) {
            return;
        }

        model.rightArm.visible = true;
        model.leftArm.visible = true;

        model.rightSleeve.visible = true;
        model.leftSleeve.visible = true;
    }
}
