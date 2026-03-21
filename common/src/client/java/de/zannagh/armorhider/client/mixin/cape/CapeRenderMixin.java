//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.client.scopes.ItemRenderScope;
import de.zannagh.armorhider.client.scopes.ScopeFactory;
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
        var scopes = ArmorHiderClient.SCOPE_PROVIDER;
        var scope = ScopeFactory.createItemScope(scopes, null, EquipmentSlot.CHEST, avatarRenderState);
        if (scope != null) {
            scopes.enterItemRender(scope);
        }

        // When flying, the elytra is force-shown even if our mod would hide it.
        // Cancel the cape so both don't render at the same time.
        if (avatarRenderState instanceof IdentityCarrier carrier
                && Boolean.TRUE.equals(carrier.armorHider$isPlayerFlying())
                && ItemsUtil.itemStackContainsElytra(avatarRenderState.chestEquipment)) {
            var entityScope = scopes.entityScope();
            if (entityScope != null && entityScope.resolvedPlayerName() != null
                    && ItemRenderScope.isSlotFullyHidden(entityScope.resolvedPlayerName(), EquipmentSlot.CHEST, avatarRenderState.chestEquipment)) {
                scopes.exitItemRender();
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
        if (RenderDecisions.shouldHideEquipment(ArmorHiderClient.SCOPE_PROVIDER)) {
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
            var entityScope = ArmorHiderClient.SCOPE_PROVIDER.entityScope();
            if (entityScope != null && entityScope.isPlayerEntity() && entityScope.resolvedPlayerName() != null
                    && ItemRenderScope.isSlotFullyHidden(entityScope.resolvedPlayerName(), EquipmentSlot.CHEST, item)) {
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
        ArmorHiderClient.SCOPE_PROVIDER.exitItemRender();
    }
}
//?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.client.scopes.ItemRenderScope;
import de.zannagh.armorhider.client.scopes.ScopeFactory;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
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
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void setupCapeRenderContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, PlayerRenderState playerRenderState, float f, float g, CallbackInfo ci) {
        var scopes = ArmorHiderClient.SCOPE_PROVIDER;
        var scope = ScopeFactory.createItemScope(scopes, null, EquipmentSlot.CHEST, playerRenderState);
        if (scope != null) {
            scopes.enterItemRender(scope);
        }

        if (playerRenderState instanceof IdentityCarrier carrier
                && Boolean.TRUE.equals(carrier.armorHider$isPlayerFlying())
                && ItemsUtil.itemStackContainsElytra(playerRenderState.chestEquipment)) {
            var entityScope = scopes.entityScope();
            if (entityScope != null && entityScope.resolvedPlayerName() != null
                    && ItemRenderScope.isSlotFullyHidden(entityScope.resolvedPlayerName(), EquipmentSlot.CHEST, playerRenderState.chestEquipment)) {
                scopes.exitItemRender();
                ci.cancel();
            }
        }
    }

    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
            )
    )
    private void moveCapeWhenArmorHidden(PoseStack instance, float x, float y, float z, Operation<Void> original) {
        if (RenderDecisions.shouldHideEquipment(ArmorHiderClient.SCOPE_PROVIDER)) {
            original.call(instance, 0F, 0F, 0F);
        } else {
            original.call(instance, x, y, z);
        }
    }

    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;)Z",
                    ordinal = 0
            )
    )
    private boolean bypassWingsWhenElytraHidden(CapeLayer instance, ItemStack item, EquipmentClientInfo.LayerType layerType, Operation<Boolean> original) {
        boolean result = original.call(instance, item, layerType);
        if (result) {
            var entityScope = ArmorHiderClient.SCOPE_PROVIDER.entityScope();
            if (entityScope != null && entityScope.isPlayerEntity() && entityScope.resolvedPlayerName() != null
                    && ItemRenderScope.isSlotFullyHidden(entityScope.resolvedPlayerName(), EquipmentSlot.CHEST, item)) {
                return false;
            }
        }
        return result;
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V",
            at = @At("RETURN")
    )
    private void releaseCapeContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, PlayerRenderState playerRenderState, float f, float g, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.exitItemRender();
    }
}
*///?}

//? if < 1.21.4 {
/*package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.scopes.ItemRenderScope;
import de.zannagh.armorhider.client.scopes.ScopeFactory;
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
        var scopes = ArmorHiderClient.SCOPE_PROVIDER;
        var scope = ScopeFactory.createItemScope(scopes, null, EquipmentSlot.CHEST, player);
        if (scope != null) {
            scopes.enterItemRender(scope);
        }

        if ((player.isFallFlying() || player.getAbilities().flying)
                && ItemsUtil.itemStackContainsElytra(player.getItemBySlot(EquipmentSlot.CHEST))) {
            var entityScope = scopes.entityScope();
            if (entityScope != null && entityScope.resolvedPlayerName() != null
                    && ItemRenderScope.isSlotFullyHidden(entityScope.resolvedPlayerName(), EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST))) {
                scopes.exitItemRender();
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
        if (RenderDecisions.shouldHideEquipment(ArmorHiderClient.SCOPE_PROVIDER)) {
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
        if (RenderDecisions.shouldHideEquipment(ArmorHiderClient.SCOPE_PROVIDER)) {
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
            var entityScope = ArmorHiderClient.SCOPE_PROVIDER.entityScope();
            if (entityScope != null && entityScope.isPlayerEntity() && entityScope.resolvedPlayerName() != null
                    && ItemRenderScope.isSlotFullyHidden(entityScope.resolvedPlayerName(), EquipmentSlot.CHEST, instance)) {
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
        ArmorHiderClient.SCOPE_PROVIDER.exitItemRender();
    }
}
*///?}
