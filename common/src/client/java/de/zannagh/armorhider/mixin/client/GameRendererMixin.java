//? if >= 1.21 {
package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sets scope flags around GameRenderer.render() and GameRenderer.renderLevel()
 * so that {@link EquipmentSlotHidingMixin} can distinguish level rendering
 * (3D world) from HUD/GUI rendering and game logic.
 * <p>
 * The render-frame scope covers the entire render() call (world + HUD + GUI).
 * The level-render scope covers only renderLevel() (3D world rendering).
 * Equipment slot hiding uses the narrower level-render scope so that HUD mods
 * (e.g. DurabilityViewer) still see the real equipment items.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void enterRenderFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.enterRenderFrame();
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("RETURN"))
    private void exitRenderFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.exitRenderFrame();
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void enterLevelRender(DeltaTracker deltaTracker, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.enterLevelRender();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void exitLevelRender(DeltaTracker deltaTracker, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.exitLevelRender();
    }
}
//?}

//? if < 1.21 {
/*package de.zannagh.armorhider.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render(FJZ)V", at = @At("HEAD"))
    private void enterRenderFrame(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.enterRenderFrame();
    }

    @Inject(method = "render(FJZ)V", at = @At("RETURN"))
    private void exitRenderFrame(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.exitRenderFrame();
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void enterLevelRender(float partialTick, long nanoTime, PoseStack poseStack, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.enterLevelRender();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void exitLevelRender(float partialTick, long nanoTime, PoseStack poseStack, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.exitLevelRender();
    }
}
*///?}
