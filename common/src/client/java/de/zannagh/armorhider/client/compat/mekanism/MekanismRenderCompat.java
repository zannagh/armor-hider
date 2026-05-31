//? if mekanism {
/*package de.zannagh.armorhider.client.compat.mekanism;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.compat.figura.FiguraCompat;
import mekanism.client.render.MekanismRenderType;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public final class MekanismRenderCompat {

    private static RenderType MEKASUIT_ORIGINAL;
    private static boolean initialized;

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        MEKASUIT_ORIGINAL = MekanismRenderType.MEKASUIT;
    }

    public static MultiBufferSource wrapForTransparency(MultiBufferSource original) {
        init();
        if (MEKASUIT_ORIGINAL == null) {
            return original;
        }
        RenderType translucentType = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
        return (RenderType type) -> {
            if (type == MEKASUIT_ORIGINAL) {
                return original.getBuffer(translucentType);
            }
            return original.getBuffer(type);
        };
    }

    @SuppressWarnings("unchecked")
    public static <T extends LivingEntity> void renderBodyUnderArmor(
            EntityModel<?> parentModel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            T entity,
            EquipmentSlot slot,
            int light) {
        if (!(entity instanceof AbstractClientPlayer player)
            || !(parentModel instanceof PlayerModel<?> model)) {
            return;
        }
        if (ArmorHiderClient.FIGURA_LOADED && FiguraCompat.hasAvatar(player)) {
            return;
        }
        var skin = player.getSkin().texture();
        var vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skin));

        boolean headVis = model.head.visible;
        boolean hatVis = model.hat.visible;
        boolean bodyVis = model.body.visible;
        boolean leftArmVis = model.leftArm.visible;
        boolean rightArmVis = model.rightArm.visible;
        boolean leftLegVis = model.leftLeg.visible;
        boolean rightLegVis = model.rightLeg.visible;
        boolean jacketVis = model.jacket.visible;
        boolean leftSleeveVis = model.leftSleeve.visible;
        boolean rightSleeveVis = model.rightSleeve.visible;
        boolean leftPantsVis = model.leftPants.visible;
        boolean rightPantsVis = model.rightPants.visible;

        model.setAllVisible(false);

        boolean changeVisibility = true;
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.leftArm.visible = true;
                model.rightArm.visible = true;
                model.jacket.visible = true;
                model.leftSleeve.visible = true;
                model.rightSleeve.visible = true;
            }
            case LEGS, FEET -> {
                model.leftLeg.visible = true;
                model.rightLeg.visible = true;
                model.leftPants.visible = true;
                model.rightPants.visible = true;
            }
            default -> {
                changeVisibility = false;
            }
        }
        
        if (!changeVisibility) {
            return;
        }

        poseStack.pushPose();
        if (slot == EquipmentSlot.CHEST) {
            poseStack.scale(0.99f, 0.99f, 0.99f); // Slightly shrink chest to prevent overlap-flickers.
        }
        model.renderToBuffer(poseStack, vc, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();

        model.head.visible = headVis;
        model.hat.visible = hatVis;
        model.body.visible = bodyVis;
        model.leftArm.visible = leftArmVis;
        model.rightArm.visible = rightArmVis;
        model.leftLeg.visible = leftLegVis;
        model.rightLeg.visible = rightLegVis;
        model.jacket.visible = jacketVis;
        model.leftSleeve.visible = leftSleeveVis;
        model.rightSleeve.visible = rightSleeveVis;
        model.leftPants.visible = leftPantsVis;
        model.rightPants.visible = rightPantsVis;
    }
}
*///?}
