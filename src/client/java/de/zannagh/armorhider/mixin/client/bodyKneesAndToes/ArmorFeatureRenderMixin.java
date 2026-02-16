// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

//? if >= 1.21.9 {
package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class ArmorFeatureRenderMixin {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private <S extends HumanoidRenderState> void captureContext(
            PoseStack poseStack, SubmitNodeCollector submitNodeCollector, net.minecraft.world.item.ItemStack itemStack, net.minecraft.world.entity.EquipmentSlot equipmentSlot, int i, S humanoidRenderState, CallbackInfo ci) {
        if ((humanoidRenderState instanceof ArmorStandRenderState)) {
            return;
        }
        if (itemStack.is(Items.AIR)) {
            return;
        }
        ArmorRenderPipeline.setupContext(itemStack, equipmentSlot, humanoidRenderState);
    }
}
//?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class ArmorFeatureRenderMixin {

    // Capture render state from the render() method since renderArmorPiece doesn't receive it in 1.21.4
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD")
    )
    private <S extends HumanoidRenderState> void captureRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S humanoidRenderState, float f, float g, CallbackInfo ci) {
        ArmorRenderPipeline.CURRENT_ENTITY_RENDER_STATE.set(humanoidRenderState);
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private void captureContext(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, HumanoidModel<?> model, CallbackInfo ci) {
        var renderState = ArmorRenderPipeline.CURRENT_ENTITY_RENDER_STATE.get();
        if (renderState instanceof ArmorStandRenderState) {
            return;
        }
        if (itemStack.is(Items.AIR)) {
            return;
        }
        if (renderState instanceof HumanoidRenderState humanoidState) {
            ArmorRenderPipeline.setupContext(itemStack, equipmentSlot, humanoidState);
        }
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("RETURN")
    )
    private <S extends HumanoidRenderState> void clearRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S humanoidRenderState, float f, float g, CallbackInfo ci) {
        ArmorRenderPipeline.CURRENT_ENTITY_RENDER_STATE.remove();
    }
}
*///?}

//? if < 1.21.4 {
/*package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

// This mixin is not needed in 1.20.x - HumanoidArmorLayerMixin handles context capture
public class ArmorFeatureRenderMixin {
    // Empty - context capture is done in HumanoidArmorLayerMixin for 1.20.x
}
*///?}

