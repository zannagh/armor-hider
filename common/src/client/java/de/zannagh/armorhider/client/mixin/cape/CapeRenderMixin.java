package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.RenderScopeContext;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.log.DebugLogger;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.world.entity.EquipmentSlot;
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
                                        CallbackInfo ci,
                                        @Share(value = "scopeContext") LocalRef<RenderScopeContext> scopeContext) {
        //?} else {
    /*private void setupCapeRenderContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, AbstractClientPlayer avatarRenderState, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci,
                                        @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
     *///?}
        if (!(avatarRenderState instanceof IdentityCarrier carrier)) {
            return;
        }

        //? if >= 1.21.4
        var chestEquipment = avatarRenderState.chestEquipment;
        //? if < 1.21.4
        //var chestEquipment = avatarRenderState.getItemBySlot(EquipmentSlot.CHEST);

        var api = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        var ctx = api.enterScope(RenderScope.CAPE, carrier, EquipmentSlot.CHEST, chestEquipment);
        scopeContext.set(ctx);

        if (ctx.isEmpty()) {
            api.exitScope(RenderScope.CAPE);
            return;
        }

        if (ctx.shouldCancel()) {
            if (new ItemInfo(chestEquipment).isElytra() && carrier.isPlayerFlying()) {
                DebugLogger.log("CapeRendering: Player is flying with hidden elytra, suppressing cape rendering temporarily.");
                api.exitScope(RenderScope.CAPE);
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
    private void moveCapeWhenArmorHidden(PoseStack instance, float f, float g, float h, Operation<Void> original, @Share(value = "scopeContext") LocalRef<RenderScopeContext> scopeContext) {
        if (scopeContext.get() == null || scopeContext.get().isEmpty()) {
            original.call(instance, f, g, h);
            return;
        }
        if (scopeContext.get().modification().shouldHide()) {
            DebugLogger.log("CapeRendering: Slot will be hidden downstream, moving cape back to body.");
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
                                                Operation<Boolean> original, @Share(value = "scopeContext") LocalRef<RenderScopeContext> scopeContext) {
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
                                                Operation<Boolean> original, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
        boolean result = original.call(instance, item);
    *///?}
        if (result) {
            var mod = scopeContext.get() == null ? null : scopeContext.get().modification();
            if (mod != null && mod.shouldHide()) {
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
                                    CallbackInfo ci,
                                    @Share(value = "scopeContext") LocalRef<RenderScopeContext> scopeContext) {
    //?} else {
    /*private void releaseCapeContext(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci,
                                    @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
    *///?}
        scopeContext.set(null);
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.CAPE);
    }
}
