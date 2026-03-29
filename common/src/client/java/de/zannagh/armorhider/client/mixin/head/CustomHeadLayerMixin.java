//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.constants.MixinConstants;
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
        var mod = setupContextBasedOnWornHeadType(livingEntityRenderState);
        if (mod != null && mod.shouldHide()) {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
            ci.cancel();
        }
    }

    @Inject(
            method = "resolveSkullRenderType",
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            at = @At("HEAD")
    )
    private void grabSkullRenderContext(LivingEntityRenderState livingEntityRenderState, SkullBlock.Type type, CallbackInfoReturnable<RenderType> cir) {
        if (!ArmorHiderClient.RENDER_CONTEXT.hasActiveModification()) {
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
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }

    @Unique
    private static ActiveModification setupContextBasedOnWornHeadType(LivingEntityRenderState livingEntityRenderState) {
        if (!(livingEntityRenderState instanceof HumanoidRenderState humanoidState)) {
            return null;
        }
        boolean playerIsWearingCustomItem = humanoidState.headEquipment.isEmpty()
                && humanoidState.wornHeadType == null
                && humanoidState.wornHeadProfile == null
                && humanoidState instanceof IdentityCarrier carrier
                && carrier.armorHider$customHeadItem() != null
                && !carrier.armorHider$customHeadItem().isEmpty();

        if (!playerIsWearingCustomItem
                && humanoidState.wornHeadProfile == null
                && humanoidState.wornHeadType == null) {
            return null;
        }
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        ItemStack headItem;
        if (humanoidState.wornHeadProfile != null) {
            headItem = new ItemStack(Items.PLAYER_HEAD);
        } else {
            headItem = ItemsUtil.getItemStackFromSkullBlockType(humanoidState.wornHeadType);
        }
        if (playerIsWearingCustomItem
            && humanoidState instanceof IdentityCarrier carrier) {
            headItem = carrier.armorHider$customHeadItem();
        }
        var mod = ActiveModification.forCarrier(humanoidState, EquipmentSlot.HEAD, headItem);
        if (mod != null) {
            ctx.setActiveModification(mod);
        }
        return mod;
    }
}
//?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.constants.MixinConstants;
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
        var mod = setupContextBasedOnWornHeadType(livingEntityRenderState);
        if (mod != null && mod.shouldHide()) {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            at = @At("TAIL")
    )
    private <S extends LivingEntityRenderState> void releaseContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }

    @Unique
    private static ActiveModification setupContextBasedOnWornHeadType(LivingEntityRenderState livingEntityRenderState) {
        if (!(livingEntityRenderState instanceof HumanoidRenderState humanoidState)) {
            return null;
        }
        boolean playerIsWearingCustomItem = humanoidState.wornHeadType == null
                && humanoidState.wornHeadProfile == null
                && humanoidState instanceof IdentityCarrier carrier
                && carrier.armorHider$customHeadItem() != null
                && !carrier.armorHider$customHeadItem().isEmpty();

        if (!playerIsWearingCustomItem
                && humanoidState.wornHeadProfile == null
                && humanoidState.wornHeadType == null) {
            return null;
        }
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        ItemStack headItem;
        if (humanoidState.wornHeadProfile != null) {
            headItem = new ItemStack(Items.PLAYER_HEAD);
        } else {
            headItem = ItemsUtil.getItemStackFromSkullBlockType(humanoidState.wornHeadType);
        }
        if (playerIsWearingCustomItem
            && humanoidState instanceof IdentityCarrier carrier) {
            headItem = carrier.armorHider$customHeadItem();
        }
        var mod = ActiveModification.forCarrier(humanoidState, EquipmentSlot.HEAD, headItem);
        if (mod != null) {
            ctx.setActiveModification(mod);
        }
        return mod;
    }
}
*///?}

//? if < 1.21.4 {
/*package de.zannagh.armorhider.client.mixin.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.constants.MixinConstants;
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
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        var mod = ActiveModification.forCarrier(entity, EquipmentSlot.HEAD, entity.getItemBySlot(EquipmentSlot.HEAD));
        if (mod != null) {
            ctx.setActiveModification(mod);
        }
        if (mod != null && mod.shouldHide()) {
            ctx.clearActiveModification();
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            at = @At("TAIL")
    )
    private void releaseContext(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
}
*///?}
