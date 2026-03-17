//? if >= 1.21 && < 1.21.4 {
/*package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.scopes.ScopeFactory;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// NeoForge-specific armor rendering mixin for 1.21/1.21.1.
// NeoForge patches HumanoidArmorLayer to add a 12-parameter overload of renderArmorPiece
// with extra float parameters (limb swing, partial tick, etc.). The render() method calls
// this NeoForge overload directly, bypassing the vanilla 6-parameter version. Since the
// common HumanoidArmorLayerMixin only targets the vanilla signature, its injections never
// fire on NeoForge. This mixin targets the NeoForge-specific overload.
@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(HumanoidArmorLayer.class)
public class NeoForgeHumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {

    @Inject(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ItemStack itemStack = entity.getItemBySlot(slot);
        if (itemStack.is(Items.AIR)) {
            return;
        }

        var scopes = ArmorHiderClient.SCOPE_PROVIDER;
        var scope = ScopeFactory.createItemScope(scopes, itemStack, slot, entity);
        if (scope != null) {
            scopes.enterItemRender(scope);
        }

        if (RenderDecisions.shouldCancelRender(scopes)) {
            scopes.exitItemRender();
            ci.cancel();
        }
    }

    @Inject(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("RETURN")
    )
    private void onRenderArmorPieceReturn(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.exitItemRender();
    }
}
*///?}
