//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public class CapeRenderMixin {

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void setupCapeRenderContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, AvatarRenderState avatarRenderState, float f, float g, CallbackInfo ci) {
        var mod = ((IdentityCarrier) avatarRenderState).createModification(EquipmentSlot.CHEST, null);

        if (avatarRenderState instanceof IdentityCarrier carrier
                && carrier.isPlayerFlying()
                && ItemsUtil.itemStackContainsElytra(avatarRenderState.chestEquipment)) {
            if (mod != null
                    && ActiveModification.isSlotFullyHidden(mod.playerName(), EquipmentSlot.CHEST, avatarRenderState.chestEquipment)) {
                ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
                ci.cancel();
            }
        }
    }

    @WrapOperation(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
            )
    )
    private void moveCapeWhenArmorHidden(PoseStack instance, float x, float y, float z, Operation<Void> original) {
        var mod = ArmorHiderClient.RENDER_CONTEXT.activeModification();
        if (mod != null && mod.shouldHide()) {
            // Move cape back to body when armor is hidden (no offset needed)
            original.call(instance, 0F, 0F, 0F);
        } else {
            original.call(instance, x, y, z);
        }
    }

    @WrapOperation(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;)Z",
                    ordinal = 0
            )
    )
    private boolean bypassWingsWhenElytraHidden(CapeLayer instance, ItemStack item, EquipmentClientInfo.LayerType layerType, Operation<Boolean> original) {
        boolean result = original.call(instance, item, layerType);
        if (result) {
            var mod = ArmorHiderClient.RENDER_CONTEXT.activeModification();
            if (mod != null
                    && ActiveModification.isSlotFullyHidden(mod.playerName(), EquipmentSlot.CHEST, item)) {
                return false;
            }
        }
        return result;
    }

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("RETURN")
    )
    private void releaseCapeContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, AvatarRenderState avatarRenderState, float f, float g, CallbackInfo ci) {
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
}
//?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public class CapeRenderMixin {

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void setupCapeRenderContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, AvatarRenderState playerRenderState, float f, float g, CallbackInfo ci) {
        var mod = ((IdentityCarrier) playerRenderState).createModification(EquipmentSlot.CHEST, null);

        if (playerRenderState instanceof IdentityCarrier carrier
                && carrier.isPlayerFlying()
                && ItemsUtil.itemStackContainsElytra(playerRenderState.chestEquipment)) {
            if (mod != null
                    && ActiveModification.isSlotFullyHidden(mod.playerName(), EquipmentSlot.CHEST, playerRenderState.chestEquipment)) {
                ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
                ci.cancel();
            }
        }
    }

    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
            )
    )
    private void moveCapeWhenArmorHidden(PoseStack instance, float x, float y, float z, Operation<Void> original) {
        var mod = ArmorHiderClient.RENDER_CONTEXT.activeModification();
        if (mod != null && mod.shouldHide()) {
            original.call(instance, 0F, 0F, 0F);
        } else {
            original.call(instance, x, y, z);
        }
    }

    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;)Z",
                    ordinal = 0
            )
    )
    private boolean bypassWingsWhenElytraHidden(CapeLayer instance, ItemStack item, EquipmentClientInfo.LayerType layerType, Operation<Boolean> original) {
        boolean result = original.call(instance, item, layerType);
        if (result) {
            var mod = ArmorHiderClient.RENDER_CONTEXT.activeModification();
            if (mod != null
                    && ActiveModification.isSlotFullyHidden(mod.playerName(), EquipmentSlot.CHEST, item)) {
                return false;
            }
        }
        return result;
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("RETURN")
    )
    private void releaseCapeContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, AvatarRenderState playerRenderState, float f, float g, CallbackInfo ci) {
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
}
*///?}

//? if < 1.21.4 {
/*package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public class CapeRenderMixin {

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void setupCapeRenderContext(PoseStack poseStack, MultiBufferSource bufferSource, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        var mod = ((IdentityCarrier) player).createModification(EquipmentSlot.CHEST, null);

        if (player instanceof IdentityCarrier carrier
                && carrier.isPlayerFlying()
                && ItemsUtil.itemStackContainsElytra(player.getItemBySlot(EquipmentSlot.CHEST))) {
            if (mod != null
                    && ActiveModification.isSlotFullyHidden(mod.playerName(), EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST))) {
                ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
                ci.cancel();
            }
        }
    }

    //? if >= 1.21 {

    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
            )
    )
    private void moveCapeWhenArmorHidden(PoseStack instance, float x, float y, float z, Operation<Void> original) {
        var mod = ArmorHiderClient.RENDER_CONTEXT.activeModification();
        if (mod != null && mod.shouldHide()) {
            original.call(instance, 0F, 0F, 0F);
        } else {
            original.call(instance, x, y, z);
        }
    }
    //?}

    //? if < 1.21 {
    /^@WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
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
    ^///?}

    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"
            )
    )
    private boolean bypassElytraCheckWhenHidden(ItemStack instance, Item item, Operation<Boolean> original) {
        boolean result = original.call(instance, item);
        if (result && item == Items.ELYTRA) {
            var mod = ArmorHiderClient.RENDER_CONTEXT.activeModification();
            if (mod != null
                    && ActiveModification.isSlotFullyHidden(mod.playerName(), EquipmentSlot.CHEST, instance)) {
                return false;
            }
        }
        return result;
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At("RETURN")
    )
    private void releaseCapeContext(PoseStack poseStack, MultiBufferSource bufferSource, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
}
*///?}
