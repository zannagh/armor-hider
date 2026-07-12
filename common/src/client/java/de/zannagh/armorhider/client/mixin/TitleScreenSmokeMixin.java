package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.gui.screens.AdvancedArmorHiderSettingsScreen;
import de.zannagh.armorhider.client.gui.screens.ArmorHiderOptionsScreen;
import de.zannagh.armorhider.client.gui.screens.IndividualPlayerConfigurationsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Boot-smoke only: when the client boots with {@code -Darmorhider.smoke.exit=true} (Phase 1 boot smoke —
 * the only automated coverage NeoForge and the pre-1.21.4 Fabric versions get, since FCGT is Fabric 1.21.4+
 * only), open each ArmorHider screen once from the title screen so its init + first render actually run on
 * that loader/version. A crash in any screen's init (e.g. the compound-button-row array overflow) then fails
 * the boot smoke on that exact variant instead of slipping through as "booted fine".
 *
 * <p>No-op in normal runs and during FCGT: the FCGT run task deliberately sets the property to {@code false}
 * so this doesn't fight the game-test driver, and FCGT drives the screens itself.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenSmokeMixin {

    @Unique
    private static boolean armorHider$smokeScreensOpened = false;

    @Inject(method = "init", at = @At("TAIL"))
    private void armorHider$openScreensForSmoke(CallbackInfo ci) {
        if (!Boolean.getBoolean("armorhider.smoke.exit") || armorHider$smokeScreensOpened) {
            return;
        }
        armorHider$smokeScreensOpened = true;

        // Defer onto the client executor so we don't switch screens mid-init, then chain one screen per tick
        // so each fully initialises and renders at least one frame before the next opens.
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ArmorHider.LOGGER.info("[smoke] opening ArmorHider options screen");
            mc.setScreenAndShow(new ArmorHiderOptionsScreen(null, mc.options));
            mc.execute(() -> {
                ArmorHider.LOGGER.info("[smoke] opening ArmorHider individual/global screen");
                mc.setScreenAndShow(new IndividualPlayerConfigurationsScreen(
                        null, mc.options, Component.translatable("armorhider.individual.title")));
                mc.execute(() -> {
                    ArmorHider.LOGGER.info("[smoke] opening ArmorHider advanced screen");
                    mc.setScreenAndShow(new AdvancedArmorHiderSettingsScreen(
                            null, mc.options, Component.translatable("armorhider.options.mod_title")));
                    ArmorHider.LOGGER.info("[smoke] ArmorHider screens opened without crashing");
                });
            });
        });
    }
}
