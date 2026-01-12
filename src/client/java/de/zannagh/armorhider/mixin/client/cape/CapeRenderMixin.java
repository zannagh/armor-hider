package de.zannagh.armorhider.mixin.client.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public abstract class CapeRenderMixin extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    @Unique
    private final ThreadLocal<AbstractClientPlayerEntity> playerEntityRenderState = new ThreadLocal<>();

    public CapeRenderMixin(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }


    @Inject(
            at = @At("HEAD"),
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V"
    )
    private void setupCapeRenderContext(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci){
        ArmorRenderPipeline.setupContext(abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST), EquipmentSlot.CHEST, abstractClientPlayerEntity);
        this.playerEntityRenderState.set(abstractClientPlayerEntity);
        if (!ArmorRenderPipeline.shouldModifyEquipment() || ArmorRenderPipeline.renderStateDoesNotTargetPlayer(playerEntityRenderState)) {
            ArmorRenderPipeline.clearContext();
        }
    }

    @WrapOperation(
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/PlayerEntityModel;renderCape(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"),
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V"
    )
    private <T extends LivingEntity> void translateCape(PlayerEntityModel<T> instance, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, Operation<Void> original){
        if (!ArmorRenderPipeline.hasActiveContext() && playerEntityRenderState.get() != null) {
            // Something failed on setting up the context, so set it up via instance field.
            ArmorRenderPipeline.setupContext(playerEntityRenderState.get().getEquippedStack(EquipmentSlot.CHEST), EquipmentSlot.CHEST, playerEntityRenderState.get());
        }
        
        if (ArmorRenderPipeline.shouldHideEquipment()) {
            if (!playerEntityRenderState.get().getEquippedStack(EquipmentSlot.CHEST).isEmpty()) {
                matrices.translate(0F, 0.053125F, 0.06875F);
            }
            original.call(instance, matrices, vertices, light, overlay);
            return;
        }

        original.call(instance, matrices, vertices, light, overlay);
    }

    @Inject(
            at = @At("TAIL"),
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V"
    )
    private void releaseCapeContext(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci){
        ArmorRenderPipeline.clearContext();
        this.playerEntityRenderState.remove();
    }
}
