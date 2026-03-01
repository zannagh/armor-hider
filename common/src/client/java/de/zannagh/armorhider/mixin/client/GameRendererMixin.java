//? if >= 1.21 {
package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sets a render-frame flag around GameRenderer.render() so that
 * {@link EquipmentSlotHidingMixin} can distinguish rendering from game logic.
 * Without this, hidden armor slots appearing empty during inventory interactions
 * causes items to vanish (e.g. swapping elytra with a hidden chestplate).
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void enterRenderFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        ArmorRenderPipeline.enterRenderFrame();
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("RETURN"))
    private void exitRenderFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        ArmorRenderPipeline.exitRenderFrame();
    }
}
//?}

//? if < 1.21 {
/*package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render(FJZ)V", at = @At("HEAD"))
    private void enterRenderFrame(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        ArmorRenderPipeline.enterRenderFrame();
    }

    @Inject(method = "render(FJZ)V", at = @At("RETURN"))
    private void exitRenderFrame(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        ArmorRenderPipeline.exitRenderFrame();
    }
}
*///?}
