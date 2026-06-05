//? if < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.hand;

import de.zannagh.armorhider.client.api.render.RenderScope;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
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
        
        var scopeApi = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        var offCtx = scopeApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx = scopeApi.getActiveScope(RenderScope.HEAD);
        var activeCtx = !offCtx.isEmpty() ? offCtx : hdCtx;
        if (!activeCtx.isEmpty()) {
            return activeCtx.renderModificationApi().applyArmorTransparency(color);
        }
        return color;
    }
    //? }

    //? if < 1.21 {
    /^@ModifyVariable(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"),
            ordinal = 3,
            argsOnly = true
    )
    private float modifyRenderAlpha(float alpha) {
        
        var scopeApi2 = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        var offCtx2 = scopeApi2.getActiveScope(RenderScope.OFFHAND);
        var hdCtx2 = scopeApi2.getActiveScope(RenderScope.HEAD);
        var activeCtx2 = !offCtx2.isEmpty() ? offCtx2 : hdCtx2;
        if (!activeCtx2.isEmpty()) {
            return alpha * activeCtx2.renderModificationApi().getTransparencyAlpha();
        }
        return alpha;
    }
    ^///? }
}
*///? }
