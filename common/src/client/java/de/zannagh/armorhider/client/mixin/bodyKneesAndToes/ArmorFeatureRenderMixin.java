//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.scopes.ScopeFactory;
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
        var scopes = ArmorHiderClient.SCOPE_PROVIDER;
        var scope = ScopeFactory.createItemScope(scopes, itemStack, equipmentSlot, humanoidRenderState);
        if (scope != null) {
            scopes.enterItemRender(scope);
        }

        if (RenderDecisions.shouldCancelRender(scopes)) {
            scopes.exitItemRender();
            ci.cancel();
        }
    }
}
//?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.scopes.ScopeFactory;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
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

    // Enrich entity scope from the render() method since renderArmorPiece doesn't receive it in 1.21.4-1.21.8
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD")
    )
    private <S extends HumanoidRenderState> void captureRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S humanoidRenderState, float f, float g, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.enrichEntityScope(humanoidRenderState);
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void captureContext(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, HumanoidModel<?> model, CallbackInfo ci) {
        var scopes = ArmorHiderClient.SCOPE_PROVIDER;
        var entityScope = scopes.entityScope();
        if (entityScope == null || !entityScope.isPlayerEntity()) {
            return;
        }
        if (itemStack.is(Items.AIR)) {
            return;
        }
        // Use the entity-scope-only overload since renderArmorPiece doesn't receive
        // the render state in 1.21.4-1.21.8 (it was enriched in captureRenderState)
        var scope = ScopeFactory.createItemScope(scopes, itemStack, equipmentSlot);
        if (scope != null) {
            scopes.enterItemRender(scope);
        }

        if (RenderDecisions.shouldCancelRender(scopes)) {
            scopes.exitItemRender();
            ci.cancel();
        }
    }
}
*///?}
