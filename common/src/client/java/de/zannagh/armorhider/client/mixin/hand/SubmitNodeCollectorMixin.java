//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//? if <= 26.1.2 {
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
//?}
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.renderer.rendertype.RenderType;

@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectorMixin {

    //? if <= 26.1.2 {
    @WrapOperation(
            method = "submitModelPart",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/ModelPartFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelPartSubmit;)V"
            )
    )
    private void wrapModelPartAdd(ModelPartFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelPartSubmit submit, Operation<Void> original) {
        
        var offhandCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var headCtx = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var ctx = !offhandCtx.isEmpty() ? offhandCtx : headCtx;
        if (!ctx.isEmpty()) {
            float alpha = ctx.renderModificationApi().getTransparencyAlpha();

            SubmitNodeStorage.ModelPartSubmit modified = getModelPartSubmit(submit, alpha);

            RenderType translucentType = renderType;
            if (submit.sprite() != null) {
                translucentType =
                        ctx.renderModificationApi().getTranslucentEntityRenderType(submit.sprite().atlasLocation());
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
    //?}

    @WrapOperation(
            method = "submitModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;)V"
            )
    )

    private <S> void wrapModelAdd(ModelFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelSubmit<S> submit, Operation<Void> original) {
        
        var offCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var activeCtx = !offCtx.isEmpty() ? offCtx : hdCtx;
        if (!activeCtx.isEmpty()) {
            float alpha = activeCtx.renderModificationApi().getTransparencyAlpha();

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
                translucentType =
                        activeCtx.renderModificationApi().getTranslucentEntityRenderType(submit.sprite().atlasLocation());
            }

            original.call(storage, translucentType, modified);
        } else {
            original.call(storage, renderType, submit);
        }
    }
}
//? }
