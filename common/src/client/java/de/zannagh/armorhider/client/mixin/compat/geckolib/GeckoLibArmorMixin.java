package de.zannagh.armorhider.client.mixin.compat.geckolib;

import com.geckolib.renderer.GeoArmorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.compat.geckolib.GeckoLibRenderState;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >= 1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import com.geckolib.renderer.base.GeoRenderState;
//? } else {
/*
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
*///? }

@Pseudo
@Mixin(value = GeoArmorRenderer.class, remap = false)
public class GeckoLibArmorMixin {

    //? if >= 1.21.9 {
    @Inject(method = "tryRenderGeoArmorPiece", at = @At("HEAD"), cancellable = true)
    private static void interceptGeckoLibArmor(
            @Coerce Object modelFunction,
            PoseStack poseStack,
            SubmitNodeCollector renderTasks,
            ItemStack stack,
            EquipmentSlot slot,
            int packedLight,
            HumanoidRenderState renderState,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(renderState instanceof IdentityCarrier carrier)) {
            return;
        }
        var ctx = AhRenderManagementApi.enterScope(RenderScope.ARMOR_PIECE, carrier, slot, stack);
        var mod = ctx.modification();
        if (!mod.needsModification()) {
            return;
        }

        if (mod.shouldHide()) {
            // Cancel GeckoLib rendering entirely, so rendering is delegated down the vanilla pipeline.
            AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
            cir.setReturnValue(false);
            return;
        }

        if (renderState instanceof GeoRenderState geoState) {
            GeoRenderState perSlotState = GeckoLibRenderState.getPerSlotState(geoState, slot);
            if (perSlotState != null) {
                int originalColor = GeckoLibRenderState.getRenderColor(perSlotState);
                carrier.saveGeckoLibColor(originalColor);
                int modifiedColor = ctx.renderModificationApi().applyArmorTransparency(originalColor);
                GeckoLibRenderState.setRenderColor(perSlotState, modifiedColor);
            }
        }
    }

    @Inject(method = "tryRenderGeoArmorPiece", at = @At("RETURN"))
    private static void clearGeckoLibContext(
            @Coerce Object modelFunction,
            PoseStack poseStack,
            SubmitNodeCollector renderTasks,
            ItemStack stack,
            EquipmentSlot slot,
            int packedLight,
            HumanoidRenderState renderState,
            CallbackInfoReturnable<Boolean> cir) {
        // Restore the original render color on the per-slot state
        if (renderState instanceof IdentityCarrier carrier) {
            Integer savedColor = carrier.pollSavedGeckoLibColor();
            if (savedColor != null && renderState instanceof GeoRenderState geoState) {
                GeoRenderState perSlotState = GeckoLibRenderState.getPerSlotState(geoState, slot);
                if (perSlotState != null) {
                    GeckoLibRenderState.setRenderColor(perSlotState, savedColor);
                }
            }
        }
        AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
    }
    //? } else {
    /*
    @Shadow protected EquipmentSlot currentSlot;
    @Shadow protected ItemStack currentStack;
    @Shadow protected Entity currentEntity;
    *///? }

    // ========================
    // Render type transparency
    // ========================


    // @Inject at HEAD avoids bridge method issues — cancelling from the bridge
    // short-circuits before the real method runs, so the inject fires exactly once.
    @Inject(method = "getRenderType*", at = @At("HEAD"), cancellable = true, require = 0)
    private void modifyRenderType(
            @Coerce Object renderState,
            Identifier texture,
            //? if >= 1.21 && < 1.21.9 {
            /*@Coerce Object bufferSource,
            float partialTick,
             *///? }
            CallbackInfoReturnable<RenderType> cir) {
        //? if < 1.21.9 {
        /*
        if (currentEntity instanceof IdentityCarrier carrier && currentSlot != null) {
            AhRenderManagementApi.enterScope(RenderScope.ARMOR_PIECE, carrier, currentSlot, currentStack);
        }
         *///? }
        var armorCtx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (armorCtx.isEmpty()) {
            return;
        }
        if (armorCtx.modification().transparency() < 1.0) {
            cir.setReturnValue(
                    armorCtx.renderModificationApi().renderTypes().getTranslucentArmorRenderType(texture));
        }
    }

    // ========================
    // Color alpha on older versions
    // ========================

    //? if >= 1.21 && < 1.21.9 {
    /*
    @ModifyVariable(
        method = "actuallyRender",
        at = @At("HEAD"),
        //? if >= 1.21
        ordinal = 2,
        //? if < 1.21
        //ordinal = 4,
        argsOnly = true, require = 0)
    private int modifyRenderColor(int renderColor) {
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().applyArmorTransparency(renderColor);
    }
    *///? }

    //? if < 1.21 {
    /*
    // ========================
    // Glint suppression
    // ========================

    @WrapOperation(
            method = "renderToBuffer",
            remap = true,
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z",
                    remap = true)
    )
    private boolean modifyGlint(ItemStack instance, Operation<Boolean> original) {
        boolean result = original.call(instance);
        if (result) {
            var armorCtx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
            if (!armorCtx.isEmpty() && (armorCtx.modification().shouldDisableGlint() || armorCtx.modification().shouldHide())) {
                return false;
            }
        }
        return result;
    }
    *///? }
}
