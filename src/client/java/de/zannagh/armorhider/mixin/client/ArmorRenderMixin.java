package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.ClientConfigManager;
import de.zannagh.armorhider.PlayerConfig;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorRenderMixin {
    @Unique
    private static final ThreadLocal<BipedEntityRenderState> armorHider$currentState = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<EquipmentSlot> armorHider$currentSlot = new ThreadLocal<>();

    @Inject(method = "render", at = @At("HEAD"))
    public <S extends BipedEntityRenderState> void captureRenderState(
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int light,
            S bipedEntityRenderState,
            float limbAngle,
            float limbDistance,
            CallbackInfo ci) {
        armorHider$currentState.set(bipedEntityRenderState);
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void clearRenderState(CallbackInfo ci) {
        armorHider$currentState.remove();
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void captureSlotAndCheckHide(
            MatrixStack matrices,
            OrderedRenderCommandQueue vertexConsumers,
            ItemStack stack,
            EquipmentSlot slot,
            int light,
            BipedEntityRenderState armorModel,
            CallbackInfo ci) {
        armorHider$currentSlot.set(slot);

        PlayerConfig config = ClientConfigManager.get();
        double transparency = armorHider$getTransparencyForSlot(slot, config);

        // If transparency is near 0, completely hide the armor piece
        if (transparency < 0.01) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmor", at = @At("RETURN"))
    private void clearSlot(CallbackInfo ci) {
        armorHider$currentSlot.remove();
    }

    @Unique
    private static double armorHider$getTransparencyForSlot(EquipmentSlot slot, PlayerConfig config) {
        return switch (slot) {
            case HEAD -> config.helmetTransparency;
            case CHEST -> config.chestTransparency;
            case LEGS -> config.legsTransparency;
            case FEET -> config.bootsTransparency;
            default -> 1.0;
        };
    }
}