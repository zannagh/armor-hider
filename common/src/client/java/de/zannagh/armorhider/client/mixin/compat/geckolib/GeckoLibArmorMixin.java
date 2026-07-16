package de.zannagh.armorhider.client.mixin.compat.geckolib;

import com.geckolib.renderer.GeoArmorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.interceptors.AhGeckoLibRenderer;
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
//? } else {

/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
        var geckoLibRenderer = AhRenderInterceptionRegistryApi.getRenderer(AhGeckoLibRenderer.class);
        if (geckoLibRenderer == null) {
            return;
        }
        var result = geckoLibRenderer.intercept(renderState, slot, stack, cir);

        if (result.shouldCancel()) {
            cir.setReturnValue(false);
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
        var geckoLibRenderer = AhRenderInterceptionRegistryApi.getRenderer(AhGeckoLibRenderer.class);
        if (geckoLibRenderer == null) {
            return;
        }
        geckoLibRenderer.popAndApplyColor(renderState, slot);
    }
    //? } elif < 1.21.5 {
    
    /*// GeckoLib's field-based API (currentSlot / currentStack / currentEntity) exists only
    // on GeckoLib jars pinned for MC <= 1.21.4. On 1.21.5–1.21.8 GeckoLib still uses the
    // pre-1.21.9 MC render flow but has moved its state into per-render parameters; on
    // 1.21.9+ the whole flow changes to tryRenderGeoArmorPiece. We only @Shadow the fields
    // where they still exist.
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
        //? if < 1.21.5 {
        
        /*// Field-based armor-piece scope entry — only valid where GeckoLib still exposes
        // currentEntity / currentSlot / currentStack as shadowable fields. On 1.21.5-1.21.8
        // GeckoLib dropped those, so we rely on the HumanoidArmorLayer mixin (which fires
        // first in the layer chain) to have already entered ARMOR_PIECE scope.
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
    
    /*@ModifyVariable(
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
