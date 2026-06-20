package de.zannagh.armorhider.client.mixin.head;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.constants.MixinConstants;

import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.renderer.rendertype.RenderType;

//? if >= 1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//? } else {
import net.minecraft.client.renderer.MultiBufferSource;
//? }
//? if >= 1.21.4 {
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//? } else {
import net.minecraft.world.entity.LivingEntity;
//? }

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {

    @Unique
    private static final String ENTRY_METHOD =
            //? if >= 1.21.9 {
            "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V";
            //? } else if >= 1.21.4 {
            /*"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V";*/
            //? } else {
            /*"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V";*/
            //? }


    @Inject(
            method = ENTRY_METHOD,
            at = @At("HEAD"),
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            cancellable = true
    )
    //? if >= 1.21.4
    private <S extends LivingEntityRenderState>
    //? if < 1.21.4
    //private <S extends LivingEntity>
    void interceptHeadLayerRender
            //? if >= 1.21.9 {
            (PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S entity, float f, float g, CallbackInfo ci) {
            //? } else if >= 1.21.4 {
            /*(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, S entity, float f, float g, CallbackInfo ci) {*/
            //? } else {
            /*(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, S entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {*/
            //? }
        enterHeadScope(entity, ci);
    }

    @Inject(
            method = ENTRY_METHOD,
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            at = @At("TAIL")
    )
    private void releaseContext(CallbackInfo ci) {
        AhRenderManagementApi.exitScope(RenderScope.HEAD);
    }

    @Unique
    private static void enterHeadScope(Object state, CallbackInfo ci) {
        var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.HEAD).interceptFrom(state, ci);
        if (result.shouldCancel() || !result.shouldIntercept()) {
            return;
        }
        AhRenderManagementApi.enterScope(result);
    }

    //? if >= 1.21.9 {
    @Inject(
            method = "resolveSkullRenderType",
            //? if fabric
            order = MixinConstants.HIGH_PRIO,
            at = @At("HEAD")
    )
    private void grabSkullRenderContext(LivingEntityRenderState livingEntityRenderState, SkullBlock.Type type, CallbackInfoReturnable<RenderType> cir) {
        if (!AhRenderManagementApi.hasScopeModification(RenderScope.HEAD)) {
            enterHeadScope(livingEntityRenderState, null);
        }
    }
    //? }
}
