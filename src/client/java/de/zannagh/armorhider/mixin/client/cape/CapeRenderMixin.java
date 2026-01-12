package de.zannagh.armorhider.mixin.client.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public abstract class CapeRenderMixin extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    @Unique
    private final ThreadLocal<PlayerEntityRenderState> playerEntityRenderState = new ThreadLocal<>();
    
    public CapeRenderMixin(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }
    
    @Inject(
            at = @At("HEAD"),
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V"
    )
    private void setupCapeRenderContext(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, PlayerEntityRenderState playerEntityRenderState, float f, float g, CallbackInfo ci){
        ArmorRenderPipeline.setupContext(playerEntityRenderState.equippedChestStack, EquipmentSlot.CHEST, playerEntityRenderState);
        this.playerEntityRenderState.set(playerEntityRenderState);
        if (!ArmorRenderPipeline.shouldModifyEquipment() || ArmorRenderPipeline.renderStateDoesNotTargetPlayer(playerEntityRenderState)) {
            ArmorRenderPipeline.clearContext();
        }
    }

    @WrapOperation(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"
            ),
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V"
    )
    private void moveCapeWhenArmorHidden(MatrixStack instance, float x, float y, float z, Operation<Void> original) {
        if (!ArmorRenderPipeline.hasActiveContext() && playerEntityRenderState.get() != null) {
            // Something failed on setting up the context, so set it up via instance field.
            ArmorRenderPipeline.setupContext(playerEntityRenderState.get().equippedChestStack, EquipmentSlot.CHEST, playerEntityRenderState.get());
        }
        
        if (ArmorRenderPipeline.shouldHideEquipment()) {
            original.call(instance, 0F, 0F, 0F);
            return;
        }
        
        original.call(instance, x, y, z);
    }

    @Inject(
            at = @At("TAIL"),
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V"
    )
    private void releaseCapeContext(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, PlayerEntityRenderState playerEntityRenderState, float f, float g, CallbackInfo ci){
        ArmorRenderPipeline.clearContext();
        this.playerEntityRenderState.remove();
    }
}
