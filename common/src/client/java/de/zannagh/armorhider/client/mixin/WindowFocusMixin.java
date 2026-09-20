// Keeps the FCGT (client-gametest) window from stealing OS focus on macOS - it otherwise pops to the
// foreground when created and kicks the developer out of any fullscreen app every gametest loop. GLFW
// on macOS ignores the process-level -Dapple.awt.UIElement hint (it forces its own Regular activation
// policy), so the reliable lever is the GLFW window hints, set right before the window is created:
//  - GLFW_FOCUSED=false stops the create-time [NSApp activateIgnoringOtherApps] grab.
//  - GLFW_FOCUS_ON_SHOW=false stops the later glfwShowWindow from taking focus.
// Gated to the createGlfwWindow era (26.1.2 / 26.2): pre-26.1 keeps window creation out of
// blaze3d.platform.Window and 26.3 uses SDL (handled via SDL_WINDOW_ACTIVATE_* env vars on the run
// config), so this hook only compiles where its exact target exists. Only active under the FCGT system
// property, so normal dev/play windows still focus as usual.
//? if >= 26.1-0.snapshot.10 && < 26.3-0.snapshot.2 {
package de.zannagh.armorhider.client.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("UnusedMixin")
@Mixin(Window.class)
public class WindowFocusMixin {

    // require = 0: a purely cosmetic dev-tooling hook. If the target ever drifts we want the focus grab
    // to quietly return, not to hard-crash the client at load.
    @Inject(
            method = "createGlfwWindow",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    // createGlfwWindow returns long, so Mixin requires a CallbackInfoReturnable here even though we
    // never set a return value - we only run before the native create call and let it proceed.
    private static void armorHider$suppressGametestFocusSteal(CallbackInfoReturnable<Long> cir) {
        if (System.getProperty("fabric.client.gametest") == null) {
            return;
        }
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
    }
}
//?}
