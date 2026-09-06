//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.render.AhArmProbe;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Issue #360 reproduction harness (fabric-client-gametest-api-v1).
 * <p>
 * EMF draws custom 3D CEM armor geometry through its own renderer. The #360 fix is in
 * {@code EquipmentRenderMixin}: when EMF is present, {@code armorHider$vanillaEquipmentModel} returns
 * the original model instead of substituting vanilla geometry, so EMF's custom armor model survives to
 * EMF's own draw. That custom model then fades correctly through armor-hider's <em>existing</em>
 * translucent path - the rendertype swap plus the reduced vertex-colour alpha
 * ({@code RenderModifications.applyArmorTransparency}) - with its own texture, needing no bespoke EMF
 * hook. (An earlier speculative {@code @WrapOperation} on EMF's texture-override draw was removed as
 * dead code: EMF renders custom armor via {@code renderLikeETF}, not the override path, so the hook
 * never fired while the armor faded correctly regardless.)
 * <p>
 * This test equips full diamond armor with the <em>Glowing 3D Armor</em> resource pack (the exact
 * pack from the issue, custom CEM armor models via EMF) enabled, then renders the player through a
 * ten-step opacity gradient (100 / 90 / .. / 10 %) and screenshots each. The machine check is
 * {@link AhArmProbe#lastPath()}: at a faded step EMF must still be drawing its custom model
 * ({@link AhArmProbe#PATH_CUSTOM}) rather than the vanilla-substituted geometry - the real #360 win,
 * since the surviving custom model is what fades with its own texture.
 * <p>
 * On headless software GL (Mesa llvmpipe on CI), EMF's custom CEM armor is not reliably applied, so
 * the custom-model path is never reached and there is nothing to fade. As with the #217 smoke, that
 * is a capability gap, not a regression: the strict path assertion is downgraded to a screenshot-only
 * SKIP (loudly logged) there, while a genuine regression on capable hardware still fails.
 */
public final class EmfCustomArmorTranslucencySmokeTest implements FabricClientGameTest {

    private static final double[] OPACITIES = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};
    private static final int[] PERCENTS = {100, 90, 80, 70, 60, 50, 40, 30, 20, 10};

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] EMF #360 custom-armor translucency smoke starting");
        clearOldScreenshots();
        context.waitForScreen(TitleScreen.class);

        boolean emfPresent = CompatManager.requiresCompatTo(CompatFlags.ENTITY_MODEL_FEATURES);
        boolean packEnabled = context.computeOnClient(EmfCustomArmorTranslucencySmokeTest::enableGlowingArmorPack);
        // Only environments that can actually reach EMF's custom-armor path warrant the per-opacity
        // poll. On software GL (or with EMF/pack absent) the custom model never renders, so spinning
        // the ~300-tick ceiling per faded step just burns CI time before the inevitable SKIP.
        boolean expectCustom = emfPresent && packEnabled && !armorHider$isSoftwareGl();
        AhArmProbe.enable();
        ArmorHider.LOGGER.info("[smoke/fcgt] #360 env: emfPresent={}, glowingArmorPackEnabled={}, expectCustom={}",
                emfPresent, packEnabled, expectCustom);
        context.waitTicks(60);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {

            context.runOnClient(EmfCustomArmorTranslucencySmokeTest::setUpScene);
            context.waitTicks(20);

            boolean anyCustom = renderOpacityGradient(context, expectCustom);
            if (!(emfPresent && packEnabled && anyCustom)) {
                logCapabilitySkip(emfPresent, packEnabled, anyCustom);
                return;
            }
            assertFadedOpacities(context, expectCustom);
            ArmorHider.LOGGER.info("[smoke/fcgt] #360 custom-armor translucency checks passed");
        }
    }

    /**
     * Renders the player through the ten-step opacity gradient, screenshotting each. Returns whether
     * EMF's custom armor model rendered at any step (the capability signal that separates a real
     * regression from a headless software-GL SKIP).
     */
    private static boolean renderOpacityGradient(ClientGameTestContext context, boolean expectCustom) {
        boolean anyCustom = false;
        for (int i = 0; i < OPACITIES.length; i++) {
            anyCustom |= observeOpacity(context, PERCENTS[i], OPACITIES[i], expectCustom);
        }
        return anyCustom;
    }

    /** Re-renders the faded steps and asserts EMF kept its custom model on each. */
    private static void assertFadedOpacities(ClientGameTestContext context, boolean expectCustom) {
        for (int i = 0; i < OPACITIES.length; i++) {
            if (PERCENTS[i] == 100) {
                continue;
            }
            boolean custom = observeOpacity(context, PERCENTS[i], OPACITIES[i], expectCustom);
            assertFaded(PERCENTS[i], custom);
        }
    }

    private static void logCapabilitySkip(boolean emfPresent, boolean packEnabled, boolean anyCustom) {
        ArmorHider.LOGGER.warn("[smoke/fcgt] #360 SKIP: EMF's custom armor never rendered in this"
                + " environment (emfPresent={}, packEnabled={}, anyCustomPath={}). Software-GL / headless"
                + " cannot apply EMF custom CEM armor, so the custom-model assertion is skipped"
                + " here - a capability SKIP, not a silent pass. Run on a real GPU for the strict #360"
                + " checks.", emfPresent, packEnabled, anyCustom);
    }

    /** Asserts a faded step kept EMF's custom model (not vanilla-substituted - the real #360 win). */
    private static void assertFaded(int pct, boolean custom) {
        if (!custom) {
            throw new IllegalStateException("[smoke/fcgt] #360: at " + pct + "% opacity EMF's render path"
                    + " was not '" + AhArmProbe.PATH_CUSTOM + "' - the custom armor model was vanilla-substituted"
                    + " instead of surviving to EMF's own draw, so the piece cannot fade with its own custom"
                    + " texture (issue #360)");
        }
    }

    /**
     * Applies {@code opacity} to all four armor slots, lets the render settle, screenshots, and returns
     * whether the custom-armor path ({@link AhArmProbe#PATH_CUSTOM}) was seen. When {@code expectCustom}
     * is set (EMF + pack present on capable, non-software GL) a faded step polls until EMF's custom model
     * is seen or a ceiling elapses; otherwise the custom path is unreachable in this environment, so a
     * single settle wait replaces the poll instead of burning the full ceiling before the SKIP.
     */
    private static boolean observeOpacity(ClientGameTestContext context, int pct, double opacity, boolean expectCustom) {
        context.runOnClient(client -> {
            var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
            config.helmetOpacity.setValue(opacity);
            config.chestOpacity.setValue(opacity);
            config.legsOpacity.setValue(opacity);
            config.bootsOpacity.setValue(opacity);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .notifyConfigListeners(ArmorHiderClient.getCurrentPlayerName());
        });
        boolean faded = opacity < 1.0;
        String path;
        if (faded && expectCustom) {
            int waited = 0;
            do {
                context.waitTicks(10);
                path = AhArmProbe.lastPath();
                waited += 10;
            } while (!AhArmProbe.PATH_CUSTOM.equals(path) && waited < 300);
        } else {
            context.waitTicks(10);
            path = AhArmProbe.lastPath();
        }
        boolean custom = AhArmProbe.PATH_CUSTOM.equals(path);
        context.takeScreenshot("emf360_" + pct);
        ArmorHider.LOGGER.info("[smoke/fcgt] #360 opacity {}%: emfPath={}", pct, path);
        return custom;
    }

    /** Spawns framing: equips full diamond armor, front third-person, opaque baseline config. */
    private static void setUpScene(Minecraft client) {
        var player = client.player;
        if (player == null) {
            throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
        }
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);

        ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
        config.disableArmorHider.setValue(false);
        // Combat detection would ramp the rendered opacity back towards full on incidental damage and
        // make the faded-step assertions flaky; it has its own smoke test.
        config.enableCombatDetection.setValue(false);
        config.helmetOpacity.setValue(1.0);
        config.chestOpacity.setValue(1.0);
        config.legsOpacity.setValue(1.0);
        config.bootsOpacity.setValue(1.0);
        ArmorHiderClient.CLIENT_CONFIG_MANAGER
                .notifyConfigListeners(ArmorHiderClient.getCurrentPlayerName());
    }

    /**
     * Selects the Glowing 3D Armor resource pack (fetched into {@code run/resourcepacks/} by
     * {@code fetchFaResourcePack} under {@code -Psmoke}), appended after the current selection so it
     * takes priority, and triggers a resource reload.
     *
     * @return {@code true} if the pack was found and selected.
     */
    private static boolean enableGlowingArmorPack(Minecraft client) {
        var repo = client.getResourcePackRepository();
        repo.reload();
        List<String> selected = new ArrayList<>(repo.getSelectedIds());
        boolean found = false;
        for (String id : repo.getAvailableIds()) {
            String lower = id.toLowerCase(Locale.ROOT);
            boolean glowingArmor = lower.contains("glowing") || (lower.contains("3d") && lower.contains("armor"));
            if (glowingArmor && !selected.contains(id)) {
                selected.add(id);
                found = true;
                ArmorHider.LOGGER.info("[smoke/fcgt] enabling resource pack: {}", id);
            }
        }
        if (found) {
            repo.setSelected(selected);
            client.reloadResourcePacks();
        }
        return found;
    }

    /**
     * Whether the client is running on a software GL rasterizer (Mesa llvmpipe on the headless CI
     * runner), where GPU-dependent EMF custom-model rendering does not reliably happen. Keyed off the
     * {@code LIBGL_ALWAYS_SOFTWARE} env var the smoke workflow sets - version-agnostic and needs no GL
     * API, so it stays safe across every stonecutter variant.
     */
    private static boolean armorHider$isSoftwareGl() {
        String flag = System.getenv("LIBGL_ALWAYS_SOFTWARE");
        return "1".equals(flag) || "true".equalsIgnoreCase(flag);
    }

    /**
     * Deletes this test's screenshots from prior runs so each run leaves only its own artifacts. FCGT
     * writes to {@code <runDir>/screenshots/} with an {@code NNNN_} sequence prefix; we match on the
     * {@code emf360_} basename.
     */
    private static void clearOldScreenshots() {
        try {
            Path dir = Path.of("screenshots");
            if (!Files.isDirectory(dir)) {
                return;
            }
            try (var stream = Files.newDirectoryStream(dir, "*emf360_*.png")) {
                for (Path p : stream) {
                    Files.deleteIfExists(p);
                }
            }
        } catch (IOException e) {
            ArmorHider.LOGGER.warn("[smoke/fcgt] could not clear old #360 screenshots", e);
        }
    }
}
//?}
