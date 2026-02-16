//? if >= 1.21.9 {
package de.zannagh.armorhider.mixin.client.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.common.constants.MixinConstants;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//?if >= 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
//? }
//? if >= 1.21.9 && < 1.21.11 {
/*import net.minecraft.client.renderer.RenderType;
*///?}

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"),
            order = MixinConstants.HIGH_PRIO
    )
    private <S extends LivingEntityRenderState> void interceptHeadLayerRender(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        setupContextBasedOnWornHeadType(livingEntityRenderState);
    }

    @Inject(
            method = "resolveSkullRenderType",
            at = @At("HEAD"),
            order = MixinConstants.HIGH_PRIO
    )
    private void grabSkullRenderContext(LivingEntityRenderState livingEntityRenderState, SkullBlock.Type type, CallbackInfoReturnable<RenderType> cir) {
        if (ArmorRenderPipeline.noContext()) {
            // Double check the context interception
            setupContextBasedOnWornHeadType(livingEntityRenderState);
        }
    }

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("TAIL"),
            order = MixinConstants.HIGH_PRIO
    )
    private <S extends LivingEntityRenderState> void releaseContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        ArmorRenderPipeline.clearContext();
    }

    @Unique
    private static void setupContextBasedOnWornHeadType(LivingEntityRenderState livingEntityRenderState){
        if (!(livingEntityRenderState instanceof HumanoidRenderState humanoidState)) {
            return;
        }
        if (humanoidState.wornHeadProfile == null && humanoidState.wornHeadType == null) {
            return;
        }
        if (humanoidState.wornHeadProfile != null) {
            ArmorRenderPipeline.setupContext(new ItemStack(Items.PLAYER_HEAD), net.minecraft.world.entity.EquipmentSlot.HEAD, humanoidState);
            return;
        }
        ArmorRenderPipeline.setupContext(ItemsUtil.getItemStackFromSkullBlockType(humanoidState.wornHeadType), net.minecraft.world.entity.EquipmentSlot.HEAD, humanoidState);
    }
}
//?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.mixin.client.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.common.constants.MixinConstants;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"),
            order = MixinConstants.HIGH_PRIO
    )
    private <S extends LivingEntityRenderState> void interceptHeadLayerRender(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        setupContextBasedOnWornHeadType(livingEntityRenderState);
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("TAIL"),
            order = MixinConstants.HIGH_PRIO
    )
    private <S extends LivingEntityRenderState> void releaseContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        ArmorRenderPipeline.clearContext();
    }

    @Unique
    private static void setupContextBasedOnWornHeadType(LivingEntityRenderState livingEntityRenderState) {
        if (!(livingEntityRenderState instanceof HumanoidRenderState humanoidState)) {
            return;
        }
        if (humanoidState.wornHeadProfile == null && humanoidState.wornHeadType == null) {
            return;
        }
        if (humanoidState.wornHeadProfile != null) {
            ArmorRenderPipeline.setupContext(new ItemStack(Items.PLAYER_HEAD), EquipmentSlot.HEAD, humanoidState);
            return;
        }
        ArmorRenderPipeline.setupContext(ItemsUtil.getItemStackFromSkullBlockType(humanoidState.wornHeadType), EquipmentSlot.HEAD, humanoidState);
    }
}
*///?}

//? if < 1.21.4 {
/*package de.zannagh.armorhider.mixin.client.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.common.constants.MixinConstants;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin<T extends LivingEntity> {

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            order = MixinConstants.HIGH_PRIO
    )
    private void interceptHeadLayerRender(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (ArmorRenderPipeline.entityIsNotPlayer(entity)) {
            return;
        }
        
        ArmorRenderPipeline.setupContext(entity.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD, entity);
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("TAIL"),
            order = MixinConstants.HIGH_PRIO
    )
    private void releaseContext(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ArmorRenderPipeline.clearContext();
    }
}
*///?}
