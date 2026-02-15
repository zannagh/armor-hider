//? if <= 1.21.6 {
/*package de.zannagh.armorhider.mixin.client.hand;

import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
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
        if (ArmorRenderPipeline.hasActiveContext() && ArmorRenderPipeline.shouldModifyEquipment()) {
            return ArmorRenderPipeline.applyArmorTransparency(color);
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
        if (ArmorRenderPipeline.hasActiveContext() && ArmorRenderPipeline.shouldModifyEquipment()) {
            return alpha * ArmorRenderPipeline.getTransparencyAlpha();
        }
        return alpha;
    }
    ^///? }
}
*///? }
