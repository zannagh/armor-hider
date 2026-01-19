package de.zannagh.armorhider.mixin.client.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public class CapeRenderMixin {

    @Unique
    private static final ThreadLocal<AvatarRenderState> currentPlayerRenderState = new ThreadLocal<>();

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD")
    )
    private void setupCapeRenderContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, AvatarRenderState avatarRenderState, float f, float g, CallbackInfo ci) {
        ArmorRenderPipeline.setupContext(null, EquipmentSlot.CHEST, avatarRenderState);
        currentPlayerRenderState.set(avatarRenderState);
    }

    @WrapOperation(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
            )
    )
    private void moveCapeWhenArmorHidden(PoseStack instance, float x, float y, float z, Operation<Void> original) {
        if (ArmorRenderPipeline.shouldHideEquipment()) {
            // Move cape back to body when armor is hidden (no offset needed)
            original.call(instance, 0F, 0F, 0F);
        } else {
            original.call(instance, x, y, z);
        }
    }

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("RETURN")
    )
    private void releaseCapeContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, AvatarRenderState avatarRenderState, float f, float g, CallbackInfo ci) {
        ArmorRenderPipeline.clearContext();
        currentPlayerRenderState.remove();
    }
}
