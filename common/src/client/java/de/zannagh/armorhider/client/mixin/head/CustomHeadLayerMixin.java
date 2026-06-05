//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.render.RenderScope;
import de.zannagh.armorhider.client.api.render.ScopeContext;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.constants.MixinConstants;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
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
/*import net.minecraft.client.renderer.rendertype.RenderType;
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
        var ctx = setupScopeBasedOnWornHeadType(livingEntityRenderState);
        if (ctx != null && ctx.shouldCancel()) {
            ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.HEAD);
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
        if (!ArmorHiderClientApi.getInstance().getRenderingScopeApi().hasScopeModification(RenderScope.HEAD)) {
            // Double check the context interception
            setupScopeBasedOnWornHeadType(livingEntityRenderState);
        }
    }

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            at = @At("TAIL")
    )
    private <S extends LivingEntityRenderState> void releaseContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.HEAD);
    }

    @Unique
    private static ScopeContext setupScopeBasedOnWornHeadType(LivingEntityRenderState livingEntityRenderState) {
        if (!(livingEntityRenderState instanceof IdentityCarrier carrier)) {
            return null;
        }
        if (!(carrier.customHeadItem() instanceof ItemStack headItem)) {
            return null;
        }
        if (headItem.isEmpty()) {
            return null;
        }
        var api = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        return api.enterScope(RenderScope.HEAD, carrier, EquipmentSlot.HEAD, headItem);
    }
}
//?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.render.RenderScope;
import de.zannagh.armorhider.client.api.render.ScopeContext;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.constants.MixinConstants;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
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
        var ctx = setupScopeBasedOnWornHeadType(livingEntityRenderState);
        if (ctx != null && ctx.shouldCancel()) {
            ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.HEAD);
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
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.HEAD);
    }

    @Unique
    private static ScopeContext setupScopeBasedOnWornHeadType(LivingEntityRenderState livingEntityRenderState) {
        if (!(livingEntityRenderState instanceof IdentityCarrier carrier)) {
            return null;
        }
        if (!(carrier.customHeadItem() instanceof ItemStack headItem)) {
            return null;
        }
        if (headItem.isEmpty()) {
            return null;
        }
        var api = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        return api.enterScope(RenderScope.HEAD, carrier, EquipmentSlot.HEAD, headItem);
    }
}
*///?}

//? if < 1.21.4 {
/*package de.zannagh.armorhider.client.mixin.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.render.RenderScope;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
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
        if (!(entity instanceof IdentityCarrier carrier)) {
            return;
        }
        var api = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        var ctx = api.enterScope(RenderScope.HEAD, carrier, EquipmentSlot.HEAD, entity.getItemBySlot(EquipmentSlot.HEAD));
        if (ctx.shouldCancel()) {
            api.exitScope(RenderScope.HEAD);
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
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.HEAD);
    }
}
*///?}
