package de.zannagh.armorhider.mixin.client.cape;

import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraRenderMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private <S extends BipedEntityRenderState> void interceptElytraRender(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S bipedEntityRenderState, float f, float g, CallbackInfo ci){
        ArmorHiderClient.CurrentSlot.set(EquipmentSlot.CHEST);
        ArmorHiderClient.trySetCurrentSlotFromEntityRenderState(bipedEntityRenderState);

        if (ArmorHiderClient.CurrentArmorMod.get() == null) {
            ArmorHiderClient.CurrentSlot.remove();
            return;
        }

        if (!ArmorHiderClient.CurrentArmorMod.get().ShouldModify()) {
            ArmorHiderClient.CurrentArmorMod.remove();
            ArmorHiderClient.CurrentSlot.remove();
            return;
        }

        if (ArmorHiderClient.CurrentArmorMod.get().ShouldHide()) {
            ArmorHiderClient.CurrentArmorMod.remove();
            ArmorHiderClient.CurrentSlot.remove();
            if (ci != null) {
                ci.cancel();
            }
            return;
        }

        ArmorHiderClient.CurrentArmorMod.remove();
        ArmorHiderClient.CurrentSlot.remove();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At(value = "RETURN")
    )
    private <S extends BipedEntityRenderState> void releaseContext(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S bipedEntityRenderState, float f, float g, CallbackInfo ci){
        ArmorHiderClient.CurrentArmorMod.remove();
        ArmorHiderClient.CurrentSlot.remove();
    }
}
