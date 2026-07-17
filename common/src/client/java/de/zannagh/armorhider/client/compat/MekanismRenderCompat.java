//? if mekanism {
/*package de.zannagh.armorhider.client.compat.mekanism;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.compat.FiguraCompat;
import mekanism.client.render.MekanismRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.model.rendering.PartFilterScheme;

public final class MekanismRenderCompat {

    private static RenderType MEKASUIT_ORIGINAL;
    private static boolean initialized;

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        MEKASUIT_ORIGINAL = MekanismRenderType.MEKASUIT;
    }

    @SuppressWarnings("deprecation") // TextureAtlas.LOCATION_BLOCKS: no non-deprecated Identifier alternative; Mojang's own Sheets uses it internally.
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

    public static <T extends LivingEntity> void renderBodyUnderArmor(
            EntityModel<T> parentModel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            T entity,
            EquipmentSlot slot,
            int light,
            float partialTicks) {
        if (!(entity instanceof AbstractClientPlayer player)
            || !(parentModel instanceof PlayerModel<T> model)) {
            return;
        }
        // When Figura is loaded and the player has an Avatar, Figura draws its own
        // body model — suppress the vanilla under-armor body pass for ALL slots, and
        // only delegate the head draw to Figura when the slot actually warrants it.
        if (ArmorHiderClient.FIGURA_LOADED && AvatarManager.getAvatar(player) instanceof Avatar avatar) {
            if (FiguraCompat.shouldEnforceHeadRendering(slot)) {
                avatar.headRender(poseStack, bufferSource, light, true);
            }
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

        // This is a fill-in pass: only re-draw body parts that were HIDDEN before this
        // armor pass ran. A part that is still visible was already drawn by the base
        // player model this frame (armor layers render after the body), so re-rendering
        // it here would stack a second, semi-transparent copy of the body on top of the
        // original — doubled skin layers and offset arms once an animation mod poses the
        // base pass differently. See issue #280.
        boolean anyHiddenPart = false;
        switch (slot) {
            case HEAD -> {
                if (!headVis) { model.head.visible = true; anyHiddenPart = true; }
                if (!hatVis) { model.hat.visible = true; anyHiddenPart = true; }
            }
            case CHEST -> {
                if (!bodyVis) { model.body.visible = true; anyHiddenPart = true; }
                if (!leftArmVis) { model.leftArm.visible = true; anyHiddenPart = true; }
                if (!rightArmVis) { model.rightArm.visible = true; anyHiddenPart = true; }
                if (!jacketVis) { model.jacket.visible = true; anyHiddenPart = true; }
                if (!leftSleeveVis) { model.leftSleeve.visible = true; anyHiddenPart = true; }
                if (!rightSleeveVis) { model.rightSleeve.visible = true; anyHiddenPart = true; }
            }
            case LEGS, FEET -> {
                if (!leftLegVis) { model.leftLeg.visible = true; anyHiddenPart = true; }
                if (!rightLegVis) { model.rightLeg.visible = true; anyHiddenPart = true; }
                if (!leftPantsVis) { model.leftPants.visible = true; anyHiddenPart = true; }
                if (!rightPantsVis) { model.rightPants.visible = true; anyHiddenPart = true; }
            }
            default -> {
            }
        }

        if (anyHiddenPart) {
            poseStack.pushPose();
            if (slot == EquipmentSlot.CHEST) {
                poseStack.scale(0.99f, 0.99f, 0.99f); // Slightly shrink chest to prevent overlap-flickers.
            } else if (slot == EquipmentSlot.HEAD) {
                // The MekaSuit helmet mesh is smaller than the vanilla head, so a full-size
                // fill-in head pokes/flickers through it while the helmet is partially drawn.
                // The head part pivots at the model origin, so this scales it cleanly in place.
                poseStack.scale(0.6f, 0.6f, 0.6ff);
            }
            model.renderToBuffer(poseStack, vc, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            poseStack.popPose();
        }

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
