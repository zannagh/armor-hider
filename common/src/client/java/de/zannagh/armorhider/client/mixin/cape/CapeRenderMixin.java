package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///? } else
import net.minecraft.client.renderer.MultiBufferSource;

//? if >= 1.21.2 {
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//?} else
//import net.minecraft.client.player.AbstractClientPlayer;

// EquipmentClientInfo is the 1.21.4 equipment-model rework; absent on 1.21.2/1.21.3.
//? if >= 1.21.4 {
/*import net.minecraft.client.resources.model.EquipmentClientInfo;
*///?}

@Mixin(CapeLayer.class)
public class CapeRenderMixin {

    @Unique
    //? if >= 1.21.9
    //private static final String CAPE_CONTEXT_METHOD = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V";
    //? if >= 1.21.2 && < 1.21.9
    private static final String CAPE_CONTEXT_METHOD = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V";
    //? if < 1.21.2
    //private static final String CAPE_CONTEXT_METHOD = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V";

    @Inject(method = CAPE_CONTEXT_METHOD, at = @At("HEAD"), cancellable = true)
    private void setupCapeContext(PoseStack poseStack,
                                  //? if >= 1.21.9 {
                                  /*SubmitNodeCollector submitNodeCollector,
                                  *///? } else
                                  MultiBufferSource multiBufferSource,
                                  int light,
                                  //? if >= 1.21.2 {
                                  PlayerRenderState avatarRenderState,
                                  //? } else
                                  //AbstractClientPlayer avatarRenderState,
                                  float limbSwing, float limbSwingAmount,
                                  //? if < 1.21.2 {
                                  /*float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
                                  *///? }
                                  CallbackInfo ci) {
        var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.CAPE).interceptFrom(avatarRenderState, ci);
        if (!result.shouldIntercept()) {
            return;
        }
        if (result.shouldCancel()) {
            AhRenderManagementApi.exitScope(RenderScope.CAPE);
            return;
        }
        AhRenderManagementApi.enterScope(result);
    }

    // ===== Move cape back to body when armor is hidden =====

    @WrapOperation(
            method = CAPE_CONTEXT_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
            )
    )
    private void moveCapeWhenArmorHidden(PoseStack instance, float f, float g, float h, Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.CAPE);
        if (!ctx.isEmpty() && ctx.modification().shouldHide()) {
            original.call(instance, 0F, 0F, 0F);
        } else {
            original.call(instance, f, g, h);
        }
    }


    @WrapOperation(
            method = CAPE_CONTEXT_METHOD,
            at = @At(
                    value = "INVOKE",
                    // 1.21.1: ItemStack.is(Item)  ·  1.21.2/1.21.3: CapeLayer.hasLayer(…EquipmentModel$LayerType)
                    // ·  1.21.4+: CapeLayer.hasLayer(…EquipmentClientInfo$LayerType). The equipment model API
                    // was staged: EquipmentModel (1.21.2) → EquipmentClientInfo (1.21.4).
                    //? if >= 1.21.4 {
                    /*target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;)Z",
                    *///?} elif >= 1.21.2 {
                    target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/equipment/EquipmentModel$LayerType;)Z",
                    //?} else {
                    /*target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z",
                    *///?}
                    ordinal = 0
            )
    )
    private boolean bypassWingsWhenElytraHidden(
            //? if >= 1.21.4 {
            /*CapeLayer instance,
            net.minecraft.world.item.ItemStack itemStack,
            EquipmentClientInfo.LayerType layerType,
            Operation<Boolean> original) {
        boolean result = original.call(instance, itemStack, layerType);
            *///?} elif >= 1.21.2 {
            CapeLayer instance,
            net.minecraft.world.item.ItemStack itemStack,
            net.minecraft.world.item.equipment.EquipmentModel.LayerType layerType,
            Operation<Boolean> original) {
        boolean result = original.call(instance, itemStack, layerType);
            //?} else {
            /*net.minecraft.world.item.ItemStack itemStack,
            net.minecraft.world.item.Item item,
            Operation<Boolean> original) {
        boolean result = original.call(itemStack, item);
            *///?}

        if (!result) {
            return false;
        }
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.CAPE);
        return ctx.isEmpty() || (!ctx.modification().shouldHide() && de.zannagh.armorhider.util.ItemsUtil.itemStackContainsElytra(itemStack));
    }

    @Inject(method = CAPE_CONTEXT_METHOD, at = @At("RETURN"))
    private void releaseCapeContext(CallbackInfo ci) {
        AhRenderManagementApi.exitScope(RenderScope.CAPE);
    }
}
