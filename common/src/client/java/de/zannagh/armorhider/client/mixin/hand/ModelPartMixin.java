//? if < 1.21.9 {
package de.zannagh.armorhider.client.mixin.hand;

import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(ModelPart.class)
public class ModelPartMixin {

    //? if >= 1.21 {
    @ModifyVariable(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"),
            ordinal = 2,
            argsOnly = true
    )
    private int modifyRenderColor(int color) {
        
        var offCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var activeCtx = !offCtx.isEmpty() ? offCtx : hdCtx;
        if (!activeCtx.isEmpty()) {
            return activeCtx.renderModificationApi().applyArmorTransparency(color);
        }
        return color;
    }
    //? }

    //? if < 1.21 {
    /*@ModifyVariable(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"),
            ordinal = 3,
            argsOnly = true
    )
    private float modifyRenderAlpha(float alpha) {
        
        var offCtx2 = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx2 = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var activeCtx2 = !offCtx2.isEmpty() ? offCtx2 : hdCtx2;
        if (!activeCtx2.isEmpty()) {
            return alpha * activeCtx2.renderModificationApi().getTransparencyAlpha();
        }
        return alpha;
    }
    *///? }
}
//? }
