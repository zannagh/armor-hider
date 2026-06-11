package de.zannagh.armorhider.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;

//? if >= 1.21
import net.minecraft.client.DeltaTracker;

//? if < 1.21
//import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Sets scope flags around GameRenderer.renderLevel()
 * so that {@link PlayerMixin} can distinguish level rendering
 * (3D world) from HUD/GUI rendering and game logic.
 * <p>
 * The level-render scope covers only renderLevel() (3D world rendering).
 * Equipment slot hiding uses the level-render scope so that HUD mods
 * (e.g. DurabilityViewer) still see the real equipment items.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    
    @Inject(method = "renderLevel", at = @At("HEAD"))
    //? if >= 1.21
    private void enterLevelRender(DeltaTracker deltaTracker, CallbackInfo ci) {
    //? if < 1.21
    //private void enterLevelRender(float partialTick, long nanoTime, PoseStack poseStack, CallbackInfo ci) {
        AhRenderManagementApi.setInLevelRender();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    //? if >= 1.21
    private void exitLevelRender(DeltaTracker deltaTracker, CallbackInfo ci) {
    //? if < 1.21
    //private void exitLevelRender(float partialTick, long nanoTime, PoseStack poseStack, CallbackInfo ci) {
        AhRenderManagementApi.exitInLevelRender();
    }
}
