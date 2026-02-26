//? if >= 1.21.9 {
package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//? }

//? if 1.21.9 || 1.21.10
//import net.minecraft.client.renderer.RenderType;

/**
 * NeoForge-specific armor color transparency mixin.
 * <p>
 * On Fabric, armor color is modified upstream via {@code getColorForLayer} in renderLayers.
 * NeoForge patches renderLayers and never invokes {@code getColorForLayer}, so we handle
 * armor transparency at the SubmitNodeCollection level instead — the same approach used
 * for offhand items in {@code SubmitNodeCollectorMixin}.
 */
@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(SubmitNodeCollection.class)
public class NeoForgeArmorColorMixin {

    @WrapOperation(
            method = "submitModelPart",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.11
                    target = "Lnet/minecraft/client/renderer/feature/ModelPartFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelPartSubmit;)V"
                    //? if 1.21.9 || 1.21.10
                    //target = "Lnet/minecraft/client/renderer/feature/ModelPartFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelPartSubmit;)V"
            )
    )
    //? if >= 1.21.11
    private void wrapArmorModelPartAdd(ModelPartFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelPartSubmit submit, Operation<Void> original) {
    //? if 1.21.9 || 1.21.10
    //private void wrapArmorModelPartAdd(ModelPartFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelPartSubmit submit, Operation<Void> original) {
        if (shouldApplyArmorTransparency()) {
            float alpha = ArmorRenderPipeline.getTransparencyAlpha();

            int origColor = submit.tintedColor();
            int origAlpha = (origColor >> 24) & 0xFF;
            int newAlpha = Math.round(alpha * origAlpha);
            int modifiedColor = (origColor & 0x00FFFFFF) | (newAlpha << 24);

            SubmitNodeStorage.ModelPartSubmit modified = new SubmitNodeStorage.ModelPartSubmit(
                    submit.pose(), submit.modelPart(), submit.lightCoords(), submit.overlayCoords(),
                    submit.sprite(), submit.sheeted(), submit.hasFoil(),
                    modifiedColor,
                    submit.crumblingOverlay(), submit.outlineColor()
            );

            RenderType translucentType = renderType;
            if (submit.sprite() != null) {
                //? if >= 1.21.11
                translucentType = RenderTypes.entityTranslucent(submit.sprite().atlasLocation());
                //? if 1.21.9 || 1.21.10
                //translucentType = RenderType.entityTranslucent(submit.sprite().atlasLocation());
            }

            original.call(storage, translucentType, modified);
        } else {
            original.call(storage, renderType, submit);
        }
    }

    @WrapOperation(
            method = "submitModel",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.11
                    target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;)V"
                    //? if 1.21.9 || 1.21.10
                    //target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;)V"
            )
    )
    //? if >= 1.21.11
    private <S> void wrapArmorModelAdd(ModelFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelSubmit<S> submit, Operation<Void> original) {
    //? if 1.21.9 || 1.21.10
    //private <S> void wrapArmorModelAdd(ModelFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelSubmit<S> submit, Operation<Void> original) {
        if (shouldApplyArmorTransparency()) {
            float alpha = ArmorRenderPipeline.getTransparencyAlpha();

            int origColor = submit.tintedColor();
            int origAlpha = (origColor >> 24) & 0xFF;
            int newAlpha = Math.round(alpha * origAlpha);
            int modifiedColor = (origColor & 0x00FFFFFF) | (newAlpha << 24);

            SubmitNodeStorage.ModelSubmit<S> modified = new SubmitNodeStorage.ModelSubmit<>(
                    submit.pose(), submit.model(), submit.state(),
                    submit.lightCoords(), submit.overlayCoords(), modifiedColor,
                    submit.sprite(), submit.outlineColor(), submit.crumblingOverlay()
            );

            RenderType translucentType = renderType;
            if (submit.sprite() != null) {
                //? if >= 1.21.11
                translucentType = RenderTypes.entityTranslucent(submit.sprite().atlasLocation());
                //? if 1.21.9 || 1.21.10
                //translucentType = RenderType.entityTranslucent(submit.sprite().atlasLocation());
            }

            original.call(storage, translucentType, modified);
        } else {
            original.call(storage, renderType, submit);
        }
    }

    private static boolean shouldApplyArmorTransparency() {
        return ArmorRenderPipeline.hasActiveContext(null)
                && ArmorRenderPipeline.shouldModifyEquipment()
                && ArmorRenderPipeline.getCurrentModification() != null
                && ArmorRenderPipeline.getCurrentModification().equipmentSlot() != EquipmentSlot.OFFHAND;
    }
}
//?}
