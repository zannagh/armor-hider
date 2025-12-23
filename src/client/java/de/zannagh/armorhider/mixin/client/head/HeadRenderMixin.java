package de.zannagh.armorhider.mixin.client.head;

import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeadFeatureRenderer.class)
public abstract class HeadRenderMixin<S extends LivingEntityRenderState, M extends EntityModel<S> & ModelWithHead> extends FeatureRenderer<S, M> {
    public HeadRenderMixin(FeatureRendererContext<S, M> context) {
        super(context);
    }
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"),
    cancellable = true)
    private void grabHatRenderContext(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        ArmorRenderPipeline.setupContext(null, EquipmentSlot.HEAD, livingEntityRenderState);
        
        if (!ArmorRenderPipeline.hasActiveContext()) {
            return;
        }

        if (!ArmorRenderPipeline.shouldModifyEquipment()) {
            return;
        }
        
        if (ArmorRenderPipeline.renderStateDoesNotTargetPlayer(livingEntityRenderState)) {
            return;
        }

        if (ArmorRenderPipeline.shouldHideEquipment()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V",
            at = @At("RETURN")
    )
    private void resetHatRenderContext(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        ArmorRenderPipeline.clearContext();
    }
}

