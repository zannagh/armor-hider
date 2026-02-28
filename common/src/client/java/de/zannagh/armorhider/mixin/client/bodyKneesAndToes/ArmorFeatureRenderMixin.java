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
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private <S extends HumanoidRenderState> void captureContext(
            PoseStack poseStack, SubmitNodeCollector submitNodeCollector, net.minecraft.world.item.ItemStack itemStack, net.minecraft.world.entity.EquipmentSlot equipmentSlot, int i, S humanoidRenderState, CallbackInfo ci) {
        if ((humanoidRenderState instanceof ArmorStandRenderState)) {
            return;
        }
        if (itemStack.is(Items.AIR)) {
            return;
        }
        ArmorRenderPipeline.setupContext(itemStack, equipmentSlot, humanoidRenderState);

        // Cancel the entire renderArmorPiece when armor is fully hidden (0% opacity).
        // Cancelling here improves compatibility to other mods which rely on accessing whether items are drawn at all or not.
        if (ArmorRenderPipeline.shouldCancelRender(humanoidRenderState)) {
            ArmorRenderPipeline.clearContext();
            ci.cancel();
        }
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class ArmorFeatureRenderMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("armor-hider/ArmorFeatureRender");

    // Capture render state from the render() method since renderArmorPiece doesn't receive it in 1.21.4
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD")
    )
    private <S extends HumanoidRenderState> void captureRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S humanoidRenderState, float f, float g, CallbackInfo ci) {
        ArmorRenderPipeline.CURRENT_ENTITY_RENDER_STATE.set(humanoidRenderState);
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void captureContext(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, HumanoidModel<?> model, CallbackInfo ci) {
        var renderState = ArmorRenderPipeline.CURRENT_ENTITY_RENDER_STATE.get();
        if (renderState instanceof ArmorStandRenderState
            || !(renderState instanceof HumanoidRenderState humanoidRenderState)) {
            return;
        }
        if (itemStack.is(Items.AIR)) {
            return;
        }
        // Cancel the entire renderArmorPiece when armor is fully hidden (0% opacity).
        // Cancelling here improves compatibility to other mods which rely on accessing whether items are drawn at all or not.
        if (ArmorRenderPipeline.shouldCancelRender(humanoidRenderState)) {
            // Use clearModificationContext() instead of clearContext() because in 1.21.4-1.21.8,
            // CURRENT_ENTITY_RENDER_STATE is shared across all renderArmorPiece calls within a
            // single render() invocation. Clearing it here would break subsequent slot processing.
            ArmorRenderPipeline.clearModificationContext();
            ci.cancel();
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
