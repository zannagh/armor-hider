//? if >= 1.21.11 {

package de.zannagh.armorhider.mixin.client.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Sets up and tears down the ArmorRenderPipeline context for off-hand items
/// rendered in third person. Downstream mixins (ItemRenderStateMixin for regular
/// items, ModelPartSubmitMixin for shields/banners) apply the actual transparency.
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void setupOffhandContext(ArmedEntityRenderState renderState, ItemStackRenderState itemState, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (arm == renderState.mainArm) return;
        if (!(renderState instanceof HumanoidRenderState humanoidState)) return;
        if (itemStack.isEmpty()) return;

        ArmorRenderPipeline.setupContext(itemStack, EquipmentSlot.OFFHAND, humanoidState);
    }

    @Inject(method = "submitArmWithItem", at = @At("TAIL"))
    private void clearOffhandContext(ArmedEntityRenderState renderState, ItemStackRenderState itemState, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (arm != renderState.mainArm) {
            ArmorRenderPipeline.clearContext();
        }
    }
}
//?}
