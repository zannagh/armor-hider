//? if >= 1.21 {
package de.zannagh.armorhider.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Sets a render-frame flag around GameRenderer.render() so that
 * {@link EquipmentSlotHidingMixin} can distinguish rendering from game logic.
 * Without this, hidden armor slots appearing empty during inventory interactions
 * causes items to vanish (e.g. swapping elytra with a hidden chestplate).
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @WrapMethod(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V")
    private void wrapRenderFrame(DeltaTracker deltaTracker, boolean renderLevel, Operation<Void> original) {
        ArmorRenderPipeline.enterRenderFrame();
        try {
            original.call(deltaTracker, renderLevel);
        } finally {
            ArmorRenderPipeline.exitRenderFrame();
        }
    }
}
//?}

//? if < 1.21 {
/*package de.zannagh.armorhider.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @WrapMethod(method = "render(FJZ)V")
    private void wrapRenderFrame(float partialTick, long nanoTime, boolean renderLevel, Operation<Void> original) {
        ArmorRenderPipeline.enterRenderFrame();
        try {
            original.call(partialTick, nanoTime, renderLevel);
        } finally {
            ArmorRenderPipeline.exitRenderFrame();
        }
    }
}
*///?}
