package de.zannagh.armorhider.client.mixin.compat.waveycapes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static de.zannagh.armorhider.util.ItemsUtil.itemStackContainsElytra;

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
@Mixin(targets = "dev.tr7zw.waveycapes.renderlayers.CustomCapeRenderLayer")
public class WaveyCapesMixin {

    @Unique
    //? if >= 1.21.9
    private static final String CAPE_METHOD = "submit";
    //? if < 1.21.9
    //private static final String CAPE_METHOD = "render";

    @Inject(method = CAPE_METHOD, at = @At("HEAD"), cancellable = true, remap = false)
    //? if >= 1.21.4 {
    private void setupCapeContext(PoseStack poseStack,
                                  //? if >= 1.21.9
                                  SubmitNodeCollector submitNodeCollector,
                                  //? if < 1.21.9
                                  //MultiBufferSource multiBufferSource,
                                  int light,
                                  AvatarRenderState renderState,
                                  float f, float g, CallbackInfo ci) {
        var chestEquipment = renderState.chestEquipment;
    //?} else {
    /*private void setupCapeContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        var chestEquipment = player.getItemBySlot(EquipmentSlot.CHEST);
        var renderState = player;
    *///?}
        if (renderState instanceof IdentityCarrier carrier) {
            var mod = carrier.createModification(EquipmentSlot.CHEST, chestEquipment);
            if (mod != null
                    && mod.shouldHide()
                    && itemStackContainsElytra(chestEquipment)
                    && carrier.isPlayerFlying()) {
                ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
                ci.cancel();
            }
        }
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
        var mod = ArmorHiderClient.RENDER_CONTEXT.activeModification();
        if (mod != null && mod.shouldHide()) {
            original.call(instance, 0F, 0F, 0F);
        } else {
            original.call(instance, f, g, h);
        }
    }

    @Inject(method = CAPE_METHOD, at = @At("RETURN"), remap = false)
    //? if >= 1.21.4 {
    private void releaseCapeContext(PoseStack poseStack,
                                    //? if >= 1.21.9
                                    SubmitNodeCollector submitNodeCollector,
                                    //? if < 1.21.9
                                    //MultiBufferSource multiBufferSource,
                                    int light,
                                    AvatarRenderState renderState,
                                    float f, float g, CallbackInfo ci) {
    //?} else {
    /*private void releaseCapeContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
    *///?}
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
}
