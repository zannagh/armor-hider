//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.constants.MixinConstants;
import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.scopes.ScopeFactory;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
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
//? }
//? if >= 1.21.9 && < 1.21.11 {
/*import net.minecraft.client.renderer.RenderType;
*///?}

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"),
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            cancellable = true
    )
    private <S extends LivingEntityRenderState> void interceptHeadLayerRender(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        setupContextBasedOnWornHeadType(livingEntityRenderState);
        if (RenderDecisions.shouldCancelRender(ArmorHiderClient.SCOPE_PROVIDER)) {
            ci.cancel();
            ArmorHiderClient.SCOPE_PROVIDER.exitItemRender();
        }
    }

    @Inject(
            method = "resolveSkullRenderType",
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            at = @At("HEAD")
    )
    private void grabSkullRenderContext(LivingEntityRenderState livingEntityRenderState, SkullBlock.Type type, CallbackInfoReturnable<RenderType> cir) {
        if (!ArmorHiderClient.SCOPE_PROVIDER.hasItemScope()) {
            // Double check the context interception
            setupContextBasedOnWornHeadType(livingEntityRenderState);
        }
    }

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            at = @At("TAIL")
    )
    private <S extends LivingEntityRenderState> void releaseContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.exitItemRender();
    }

    @Unique
    private static void setupContextBasedOnWornHeadType(LivingEntityRenderState livingEntityRenderState) {
        if (!(livingEntityRenderState instanceof HumanoidRenderState humanoidState)) {
            return;
        }
        if (humanoidState.wornHeadProfile == null && humanoidState.wornHeadType == null) {
            return;
        }
        var scopes = ArmorHiderClient.SCOPE_PROVIDER;
        ItemStack headItem;
        if (humanoidState.wornHeadProfile != null) {
            headItem = new ItemStack(Items.PLAYER_HEAD);
        } else {
            headItem = ItemsUtil.getItemStackFromSkullBlockType(humanoidState.wornHeadType);
        }
        var scope = ScopeFactory.createItemScope(scopes, headItem, EquipmentSlot.HEAD, humanoidState);
        if (scope != null) {
            scopes.enterItemRender(scope);
        }
    }
}
//?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.mixin.client.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.constants.MixinConstants;
import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.scopes.ScopeFactory;
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
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            cancellable = true
    )
    private <S extends LivingEntityRenderState> void interceptHeadLayerRender(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        setupContextBasedOnWornHeadType(livingEntityRenderState);
        if (RenderDecisions.shouldCancelRender(ArmorHiderClient.SCOPE_PROVIDER)) {
            ci.cancel();
            ArmorHiderClient.SCOPE_PROVIDER.exitItemRender();
        }
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            at = @At("TAIL")
    )
    private <S extends LivingEntityRenderState> void releaseContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.exitItemRender();
    }

    @Unique
    private static void setupContextBasedOnWornHeadType(LivingEntityRenderState livingEntityRenderState) {
        if (!(livingEntityRenderState instanceof HumanoidRenderState humanoidState)) {
            return;
        }
        if (humanoidState.wornHeadProfile == null && humanoidState.wornHeadType == null) {
            return;
        }
        var scopes = ArmorHiderClient.SCOPE_PROVIDER;
        ItemStack headItem;
        if (humanoidState.wornHeadProfile != null) {
            headItem = new ItemStack(Items.PLAYER_HEAD);
        } else {
            headItem = ItemsUtil.getItemStackFromSkullBlockType(humanoidState.wornHeadType);
        }
        var scope = ScopeFactory.createItemScope(scopes, headItem, EquipmentSlot.HEAD, humanoidState);
        if (scope != null) {
            scopes.enterItemRender(scope);
        }
    }
}
*///?}

//? if < 1.21.4 {
/*package de.zannagh.armorhider.mixin.client.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.constants.MixinConstants;
import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.scopes.ScopeFactory;
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
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            cancellable = true
    )
    private void interceptHeadLayerRender(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        var scopes = ArmorHiderClient.SCOPE_PROVIDER;
        var scope = ScopeFactory.createItemScope(scopes, entity.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD, entity);
        if (scope != null) {
            scopes.enterItemRender(scope);
        }
        if (RenderDecisions.shouldCancelRender(scopes)) {
            ci.cancel();
            scopes.exitItemRender();
        }
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            at = @At("TAIL")
    )
    private void releaseContext(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.exitItemRender();
    }
}
*///?}
