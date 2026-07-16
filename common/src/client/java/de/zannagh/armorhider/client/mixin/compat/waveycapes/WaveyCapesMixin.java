//? if waveycapes {
package de.zannagh.armorhider.client.mixin.compat.waveycapes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import dev.tr7zw.waveycapes.renderlayers.CustomCapeRenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;

//? if >= 1.21.4 {
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//?}

//? if < 1.21.9
//import net.minecraft.client.renderer.MultiBufferSource;

//? if < 1.21.4 {
/*import net.minecraft.client.player.AbstractClientPlayer;
*///?}

@Pseudo
@Mixin(value = CustomCapeRenderLayer.class, remap = false)
public class WaveyCapesMixin {
    @Unique
    //? if >= 1.21.9 {
    private static final String CAPE_METHOD = "submit*";
    //? } else
    //private static final String CAPE_METHOD = "render";

    @Inject(method = CAPE_METHOD, at = @At("HEAD"), cancellable = true, remap = false)
    private void setupCapeContext(PoseStack poseStack,
                                  //? if >= 1.21.9 {
                                  SubmitNodeCollector submitNodeCollector,
                                  //? } else
                                  //MultiBufferSource multiBufferSource,
                                  int light,
                                  //? if >= 1.21.4 {
                                  AvatarRenderState avatarRenderState,
                                  //? } else
                                  //AbstractClientPlayer avatarRenderState,
                                  float limbSwing, float limbSwingAmount,
                                  //? if < 1.21.4 {
                                  /*float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
                                  *///? }
                                  CallbackInfo ci) {
        //? if >= 1.21.4 {
        var chestEquipment = avatarRenderState.chestEquipment;
        //? } else
         //var chestEquipment = avatarRenderState.getItemBySlot(EquipmentSlot.CHEST);

        var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.CAPE).intercept(avatarRenderState, EquipmentSlot.CHEST, chestEquipment, ci);
        if (!result.shouldIntercept() || result.shouldCancel()) {
            return;
        }
        AhRenderManagementApi.enterScope(result);
    }

    @WrapOperation(
            method = CAPE_METHOD,
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
                    remap = true
            )
    )
    private void moveCapeWhenArmorHidden(PoseStack instance, float f, float g, float h, Operation<Void> original) {
        var capeCtx = AhRenderManagementApi.getActiveScope(RenderScope.CAPE);
        if (!capeCtx.isEmpty() && capeCtx.modification().shouldHide()) {
            original.call(instance, 0F, 0F, 0F);
        } else {
            original.call(instance, f, g, h);
        }
    }

    @Inject(method = CAPE_METHOD, at = @At("RETURN"), remap = false)
    private void releaseCapeContext(PoseStack poseStack,
                                    //? if >= 1.21.9 {
                                    SubmitNodeCollector submitNodeCollector,
                                    //? } else
                                    //MultiBufferSource multiBufferSource,
                                    int light,
                                    //? if >= 1.21.4 {
                                    AvatarRenderState renderState,
                                    //? } else
                                    //AbstractClientPlayer player,
                                    float limbSwing, float limbSwingAmount,
                                    //? if < 1.21.4 {
                                    /*float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
                                    *///? }
                                    CallbackInfo ci) {
        AhRenderManagementApi.exitScope(RenderScope.CAPE);
    }
}
//? }