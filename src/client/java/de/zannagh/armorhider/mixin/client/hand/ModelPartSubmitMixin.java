//? if >= 1.21.11 {

package de.zannagh.armorhider.mixin.client.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SubmitNodeCollection.class)
public class ModelPartSubmitMixin {

    @WrapOperation(
            method = "submitModelPart",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/ModelPartFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelPartSubmit;)V"
            )
    )
    private void wrapModelPartAdd(ModelPartFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelPartSubmit submit, Operation<Void> original) {
        if (ArmorRenderPipeline.hasActiveContext()
                && ArmorRenderPipeline.shouldModifyEquipment()
                && ArmorRenderPipeline.getCurrentModification() != null
                && ArmorRenderPipeline.getCurrentModification().equipmentSlot() == EquipmentSlot.OFFHAND) {
            float alpha = ArmorRenderPipeline.getTransparencyAlpha();

            SubmitNodeStorage.ModelPartSubmit modified = getModelPartSubmit(submit, alpha);

            // Swap render type to translucent using the sprite's atlas location
            RenderType translucentType = renderType;
            if (submit.sprite() != null) {
                translucentType = RenderTypes.entityTranslucent(submit.sprite().atlasLocation());
            }

            original.call(storage, translucentType, modified);
        } else {
            original.call(storage, renderType, submit);
        }
    }

    @Unique
    private static SubmitNodeStorage.@NonNull ModelPartSubmit getModelPartSubmit(SubmitNodeStorage.ModelPartSubmit submit, float alpha) {
        int origColor = submit.tintedColor();
        int origAlpha = (origColor >> 24) & 0xFF;
        int newAlpha = Math.round(alpha * origAlpha);
        int modifiedColor = (origColor & 0x00FFFFFF) | (newAlpha << 24);

        return new SubmitNodeStorage.ModelPartSubmit(
                submit.pose(), submit.modelPart(), submit.lightCoords(), submit.overlayCoords(),
                submit.sprite(), submit.sheeted(), submit.hasFoil(),
                modifiedColor,
                submit.crumblingOverlay(), submit.outlineColor()
        );
    }
}
//?}
