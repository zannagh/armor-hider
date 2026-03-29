//? if < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.hand;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
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
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND) || ctx.hasActiveModification(EquipmentSlot.HEAD)) {
            return RenderModifications.applyArmorTransparency(ctx, color);
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
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND) || ctx.hasActiveModification(EquipmentSlot.HEAD)) {
            return alpha * RenderModifications.getTransparencyAlpha(ctx);
        }
        return alpha;
    }
    ^///? }
}
*///? }
