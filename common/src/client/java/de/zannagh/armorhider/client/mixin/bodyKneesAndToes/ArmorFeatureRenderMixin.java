//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
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
        if (!(humanoidRenderState instanceof IdentityCarrier identityCarrier)) {
            return;
        }
        if (itemStack.is(Items.AIR)) {
            return;
        }
        var mod = identityCarrier.createModification(equipmentSlot, itemStack);
        
        if (mod != null && mod.shouldHide()) {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
            ci.cancel();
        }
    }
}
//?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
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

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD")
    )
    private <S extends HumanoidRenderState> void captureRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S humanoidRenderState, float f, float g, CallbackInfo ci) {
        if (humanoidRenderState instanceof IdentityCarrier carrier) {
            ArmorHiderClient.RENDER_CONTEXT.setCurrentPlayer(carrier.playerName());
        }
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void captureContext(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, HumanoidModel<?> model, CallbackInfo ci) {
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        String playerName = ctx.currentPlayerName();
        if (playerName == null) {
            return;
        }
        if (itemStack.is(Items.AIR)) {
            return;
        }
        var mod = ActiveModification.create(playerName, equipmentSlot, itemStack);
        if (mod != null) {
            ctx.setActiveModification(mod);
        }
        if (mod != null && mod.shouldHide()) {
            ctx.clearActiveModification();
            ci.cancel();
        }
    }
}
*///?}
