//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.compat.mekanism;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = "mekanism.client.render.layer.MekanismArmorLayer", remap = false)
public class MekanismArmorMixin {

    @Inject(method = "renderArmorPart", at = @At("HEAD"), cancellable = true, require = 0)
    private void interceptMekanismArmor(
            PoseStack poseStack,
            SubmitNodeCollector nodeCollector,
            ItemStack stack,
            EquipmentSlot slot,
            @Coerce HumanoidRenderState state,
            int lightCoords,
            CallbackInfo ci) {
        if (!(state instanceof IdentityCarrier carrier)
                || !(carrier.createModification(slot, stack) instanceof ActiveModification mod)) {
            return;
        }

        if (mod.shouldHide()) {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
            ci.cancel();
        }
    }

    @Inject(method = "renderArmorPart", at = @At("RETURN"), require = 0)
    private void clearMekanismContext(
            PoseStack poseStack,
            SubmitNodeCollector nodeCollector,
            ItemStack stack,
            EquipmentSlot slot,
            @Coerce HumanoidRenderState state,
            int lightCoords,
            CallbackInfo ci) {
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
}
//?}

//? if < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.compat.mekanism;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if < 1.21.5 {
/^import de.zannagh.armorhider.client.rendering.MekanismRenderCompat;
import org.spongepowered.asm.mixin.injection.ModifyArg;
^///?}

@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = "mekanism.client.render.layer.MekanismArmorLayer", remap = false)
public class MekanismArmorMixin {

    @Inject(method = "renderArmorPart", at = @At("HEAD"), cancellable = true, require = 0)
    private <T extends LivingEntity> void interceptMekanismArmor(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            T entity,
            EquipmentSlot slot,
            int light,
            float partialTicks,
            CallbackInfo ci) {
        if (!(entity instanceof IdentityCarrier carrier)) {
            return;
        }
        var mod = carrier.createModification(slot, entity.getItemBySlot(slot));
        if (mod != null && mod.shouldHide()) {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
            ci.cancel();
        }
    }

    //? if < 1.21.5 {
    /^@ModifyArg(
            method = "renderArmorPart",
            at = @At(value = "INVOKE",
                    target = "Lmekanism/client/render/armor/ICustomArmor;render(Lnet/minecraft/client/model/HumanoidModel;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIFZLnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"),
            index = 2,
            require = 0)
    private MultiBufferSource wrapBufferForTransparency(MultiBufferSource original) {
        var mod = ArmorHiderClient.RENDER_CONTEXT.activeModification();
        if (mod != null && mod.transparency() < 1.0) {
            return MekanismRenderCompat.wrapForTransparency(original);
        }
        return original;
    }
    ^///?}

    @Inject(method = "renderArmorPart", at = @At("RETURN"), require = 0)
    private <T extends LivingEntity> void clearMekanismContext(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            T entity,
            EquipmentSlot slot,
            int light,
            float partialTicks,
            CallbackInfo ci) {
        //? if < 1.21.5 {
        /^var mod = ArmorHiderClient.RENDER_CONTEXT.activeModification();
        if (mod != null && mod.transparency() < 1.0) {
            MekanismRenderCompat.flushTranslucentBatch(bufferSource, (float) mod.transparency());
        }
        ^///?}
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
}
*///?}
