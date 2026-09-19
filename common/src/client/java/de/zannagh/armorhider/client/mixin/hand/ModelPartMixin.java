//? if < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.hand;

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
        // This runs at the HEAD of every ModelPart.render - per model part, recursively, for every entity,
        // every frame - so the miss case has to cost as close to nothing as possible. When no scope carries
        // a modification, both getActiveScope(...) calls below would hand back an empty, no-op context and
        // the color would be returned unchanged, so bail out before querying anything.
        if (!AhRenderManagementApi.hasAnyScopeModification()) {
            return color;
        }

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
    /^@ModifyVariable(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"),
            ordinal = 3,
            argsOnly = true
    )
    private float modifyRenderAlpha(float alpha) {
        // Per model part, recursively, for every entity, every frame - see the note on modifyRenderColor.
        // With no scope carrying a modification both lookups below return an empty, no-op context.
        if (!AhRenderManagementApi.hasAnyScopeModification()) {
            return alpha;
        }

        var offCtx2 = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx2 = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var activeCtx2 = !offCtx2.isEmpty() ? offCtx2 : hdCtx2;
        if (!activeCtx2.isEmpty()) {
            return alpha * activeCtx2.renderModificationApi().getTransparencyAlpha();
        }
        return alpha;
    }
    ^///? }
}
*///? }
