package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.log.DebugLogger;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static de.zannagh.armorhider.util.ItemsUtil.itemStackContainsElytra;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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

    @Inject(
            method = CAPE_CONTEXT_METHOD,
            at = @At("HEAD"),
            cancellable = true
    )
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
        //? if >= 1.21.4
        var chestEquipment = avatarRenderState.chestEquipment;
        //? if < 1.21.4
        //var chestEquipment = avatarRenderState.getItemBySlot(EquipmentSlot.CHEST);
        if (avatarRenderState instanceof IdentityCarrier carrier)
        {
            var mod = carrier.createModification(EquipmentSlot.CHEST, chestEquipment);
            if (mod != null 
                    && mod.shouldHide() 
                    && itemStackContainsElytra(chestEquipment) 
                    && carrier.isPlayerFlying()) {
                DebugLogger.log("CapeRendering: Player is flying with hidden elytra, suppressing cape rendering temporarily.");
                    ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
                    ci.cancel();
            }
        }
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
        var mod = ArmorHiderClient.RENDER_CONTEXT.activeModification();

        if (mod != null && mod.shouldHide()) {
            DebugLogger.log("CapeRendering: Player mod is for slot {}. Slot will be hidden downstream, moving cape back to body.", mod.slot());
            original.call(instance, 0F, 0F, 0F);
        } else {
            DebugLogger.log("CapeRendering: Player mod is not present. Will not change cape rendering.");
            original.call(instance, f, g, h);
        }
    }

    @WrapOperation(
            method = CAPE_CONTEXT_METHOD,
            //? if >= 1.21.4 {
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;)Z",
                    ordinal = 0
            )
            //?} else {
            /*
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"
            )
            *///?}
    )
    private boolean bypassWingsWhenElytraHidden(CapeLayer instance,
                                                ItemStack item,
                                                //? if >= 1.21.4
                                                EquipmentClientInfo.LayerType layerType,
                                                Operation<Boolean> original) {
        //? if >= 1.21.4
        boolean result = original.call(instance, item, layerType);
        //? if < 1.21.4
        //boolean result = original.call(instance, item);
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
            method = CAPE_CONTEXT_METHOD,
            at = @At("RETURN")
    )
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
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
}
