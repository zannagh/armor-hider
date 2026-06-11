package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;

//? if >= 1.21.4 {
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
//?}

//? if < 1.21.9
//import net.minecraft.client.renderer.MultiBufferSource;

//? if < 1.21.4 {
/*import net.minecraft.client.player.AbstractClientPlayer;
*///?}

@Mixin(CapeLayer.class)
public class CapeRenderMixin {

    @Unique
    //? if >= 1.21.9
    private static final String CAPE_CONTEXT_METHOD = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V";
    //? if >= 1.21.4 && < 1.21.9
    //private static final String CAPE_CONTEXT_METHOD = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V";
    //? if < 1.21.4
    //private static final String CAPE_CONTEXT_METHOD = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V";

    @Inject(method = CAPE_CONTEXT_METHOD, at = @At("HEAD"), cancellable = true)
    //? if >= 1.21.4 {
    private void setupCapeRenderContext(PoseStack poseStack,
                                        //? if >= 1.21.9
                                        SubmitNodeCollector submitNodeCollector,
                                        //? if < 1.21.9
                                        //MultiBufferSource multiBufferSource,
                                        int light,
                                        AvatarRenderState avatarRenderState,
                                        float f,
                                        float g,
                                        CallbackInfo ci) {
    //?} else {
    /*private void setupCapeRenderContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, AbstractClientPlayer avatarRenderState, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
    *///?}
        if (!(avatarRenderState instanceof IdentityCarrier carrier)) {
            return;
        }
        
        var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.CAPE).interceptFrom(carrier, ci);
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

    //? if >= 1.21.4 {
    @WrapOperation(
            method = CAPE_CONTEXT_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;)Z",
                    ordinal = 0
            )
    )
    private boolean bypassWingsWhenElytraHidden(CapeLayer instance,
                                                net.minecraft.world.item.ItemStack item,
                                                EquipmentClientInfo.LayerType layerType,
                                                Operation<Boolean> original) {
        boolean result = original.call(instance, item, layerType);
    //?} else {
    /*@WrapOperation(
            method = CAPE_CONTEXT_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"
            )
    )
    private boolean bypassWingsWhenElytraHidden(net.minecraft.world.item.ItemStack instance,
                                                net.minecraft.world.item.Item item,
                                                Operation<Boolean> original) {
        boolean result = original.call(instance, item);
    *///?}
        if (!result) return false;
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.CAPE);
        if (!ctx.isEmpty() && ctx.modification().shouldHide()) {
            return false;
        }
        return true;
    }

    @Inject(method = CAPE_CONTEXT_METHOD, at = @At("RETURN"))
    //? if >= 1.21.4 {
    private void releaseCapeContext(PoseStack poseStack,
                                    //? if >= 1.21.9
                                    SubmitNodeCollector submitNodeCollector,
                                    //? if < 1.21.9
                                    //MultiBufferSource multiBufferSource,
                                    int light,
                                    AvatarRenderState avatarRenderState,
                                    float f,
                                    float g,
                                    CallbackInfo ci) {
    //?} else {
    /*private void releaseCapeContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
    *///?}
        AhRenderManagementApi.exitScope(RenderScope.CAPE);
    }
}
