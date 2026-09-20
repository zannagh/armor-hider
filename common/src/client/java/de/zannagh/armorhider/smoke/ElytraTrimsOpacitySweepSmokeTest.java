// Sweeps a worn ElytraTrims-decorated elytra across the whole opacity range and machine-checks every
// step. Companion to ElytraTrimsSmokeTest, which only covers the 50%/100% pair: the sweep is what
// catches a defect that appears at one particular alpha (the ET outline tint is the known one) and
// gives a human-reviewable fade progression. Same gate as its neighbour - the range where an ET submit
// wrap exists at all.
//? if fcgt && >= 1.21.9 {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ElytraTrims opacity sweep smoke (fabric-client-gametest-api-v1).
 * <p>
 * Wears the same NETHERITE/SENTRY trimmed elytra as {@link ElytraTrimsSmokeTest} and steps the elytra
 * opacity through {@code 0.0, 0.3, 0.6, 1.0}, taking one screenshot per step. Two machine checks run at
 * every step:
 * <ul>
 *   <li><b>No outline tint.</b> Each frame is pixel-scanned for the ET outline-tint signature
 *       ({@code R == 0 && G == 0 && B >= 20}) - the defect where ET reads Armor Hider's render-order
 *       value as {@code outlineColor} and paints the trim as a full-bright {@code ARGB(0,0,0,100)}
 *       outline. This is the real regression check: the call counters increment identically whether the
 *       trim drew correctly or as that outline, so only pixels can see it.</li>
 *   <li><b>Counter sanity.</b> {@link ArmorHiderRenderTypes#elytraTrimSeenCount()} must stay flat at
 *       {@code 0.0} - a hidden elytra is cancelled at the WingsLayer HEAD by
 *       {@code ArmorHiderElytraRenderer}, so ET's decorators never run - and must climb at every
 *       visible step. {@link ArmorHiderRenderTypes#elytraTrimFadeCount()} must climb at the partial
 *       steps on {@code >= 1.21.11} (ET draws translucent there) and stay flat everywhere else,
 *       including at {@code 1.0} on every version.</li>
 * </ul>
 * <p>
 * Self-skips (no fail) when ET isn't present at runtime, so run it with {@code -Pcompat=elytratrims}.
 * Pure vanilla API otherwise - ET is needed at runtime, not on the classpath.
 */
public final class ElytraTrimsOpacitySweepSmokeTest implements FabricClientGameTest {

    private static final float CAMERA_PITCH = 2.0F;
    private static final double PLAYER_Y = 100.0;

    /** Opacity steps, in sweep order, paired with the screenshot suffix each one is shot under. */
    private static final double[] SWEEP_OPACITIES = {0.0, 0.3, 0.6, 1.0};
    private static final String[] SWEEP_LABELS = {"000", "030", "060", "100"};

    /** See {@link ElytraTrimsSmokeTest} - same signature, same measured floor and budget. */
    private static final int OUTLINE_TINT_MIN_BLUE = 20;
    private static final int OUTLINE_TINT_TOLERANCE = 64;

    /**
     * Whether ET draws its trims translucently on this version, i.e. whether partial opacity is
     * expected to fade them. {@code >= 1.21.11} does; below that ET draws cutout and partial opacity is
     * a deliberate no-op (full show until 0%).
     */
    //? if >= 1.21.11 {
    private static final boolean PARTIAL_FADE_SUPPORTED = true;
    //? } else {
    /*private static final boolean PARTIAL_FADE_SUPPORTED = false;
    *///?}

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] ElytraTrims opacity sweep starting");
        clearOldScreenshots();
        context.waitForScreen(TitleScreen.class);

        if (!CompatManager.requiresCompatTo(CompatFlags.ELYTRA_TRIMS)) {
            // ET not fetched into run/mods - nothing to exercise. Skip loudly rather than fail.
            ArmorHider.LOGGER.warn("[smoke/fcgt] ElytraTrims not present - skipping sweep (run with -Pcompat=elytratrims)");
            return;
        }

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {
            ArmorHider.LOGGER.info("[smoke/fcgt] World created; equipping ET-decorated elytra for sweep");
            placePlayer(singleplayer);
            context.runOnClient(ElytraTrimsOpacitySweepSmokeTest::equipTrimmedElytra);

            // Plain tick-wait for chunks/render to settle - portable across every FCGT API version here.
            context.waitTicks(60);

            for (int i = 0; i < SWEEP_OPACITIES.length; i++) {
                sweepStep(context, SWEEP_OPACITIES[i], SWEEP_LABELS[i]);
            }

            ArmorHider.LOGGER.info("[smoke/fcgt] ElytraTrims opacity sweep complete");
        }
    }

    /** Puts the player in a fixed hovering pose the camera can shoot from run to run. */
    private static void placePlayer(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(mcServer -> {
            var sp = mcServer.getPlayerList().getPlayers().get(0);
            var abilities = sp.getAbilities();
            abilities.mayfly = true;
            abilities.flying = true;
            sp.onUpdateAbilities();
            sp.setNoGravity(true);
            sp.connection.teleport(0.5, PLAYER_Y, 0.5, 0.0F, CAMERA_PITCH);
        });
    }

    /**
     * Equips the NETHERITE/SENTRY trimmed elytra and puts the config into the state the sweep needs: the
     * elytra follows opacity, and the in-flight short-circuit is off (it keys off creative-fly too, so
     * the hovering player would otherwise keep the elytra fully visible at every step).
     */
    private static void equipTrimmedElytra(Minecraft client) {
        var player = client.player;
        if (player == null) {
            throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
        }
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        var reg = player.registryAccess();
        var material = reg.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.NETHERITE);
        var pattern = reg.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY);
        elytra.set(DataComponents.TRIM, new ArmorTrim(material, pattern));
        player.setItemSlot(EquipmentSlot.CHEST, elytra);
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        snapPose(client);

        ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
        config.opacityAffectingElytra.setValue(true);
        config.elytraInFlight.setValue(false);
    }

    /**
     * Applies one opacity step, lets the render settle, shoots it, and asserts the step's expectations.
     *
     * @param context  the running gametest context
     * @param opacity  the elytra opacity to sweep to
     * @param label    the screenshot suffix / log label for this step
     */
    private static void sweepStep(ClientGameTestContext context, double opacity, String label) {
        context.runOnClient(client -> {
            ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName())
                    .elytraOpacity.setValue(opacity);
            snapPose(client);
        });
        // Same shape as ElytraTrimsSmokeTest: settle, open the counter window, then shoot inside it.
        context.waitTicks(10);
        long seenBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.elytraTrimSeenCount());
        long fadeBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.elytraTrimFadeCount());
        context.waitTicks(6);
        Path shot = context.takeScreenshot("armorhider_et_sweep_" + label);
        long seenDelta = context.computeOnClient(client -> ArmorHiderRenderTypes.elytraTrimSeenCount()) - seenBefore;
        long fadeDelta = context.computeOnClient(client -> ArmorHiderRenderTypes.elytraTrimFadeCount()) - fadeBefore;
        ArmorHider.LOGGER.info("[smoke/fcgt] ET sweep {} (opacity {}): seenDelta={} fadeDelta={} shot {}",
                label, opacity, seenDelta, fadeDelta, shot);

        assertCounters(label, opacity, seenDelta, fadeDelta);
        assertNoOutlineTint(shot, label + " (opacity " + opacity + ")");
    }

    /**
     * Asserts the SEEN/FADE expectations for one sweep step.
     * <p>
     * At {@code 0.0} the elytra is hidden: {@code ArmorHiderElytraRenderer} cancels the WingsLayer at
     * HEAD, so ET's decorators never run and both counters must stay flat - that flat SEEN is this
     * test's "the elytra really is hidden" signal. At every visible step SEEN must climb. FADE is
     * expected only at the partial steps, and only where ET draws translucent.
     */
    private static void assertCounters(String label, double opacity, long seenDelta, long fadeDelta) {
        boolean hidden = opacity <= 0.0;
        if (hidden && seenDelta != 0) {
            throw new IllegalStateException(
                    "[smoke/fcgt] ET sweep " + label + ": elytra should be fully hidden at 0% opacity, but ET"
                            + " still drew " + seenDelta + " trim submits through our wrap - the WingsLayer"
                            + " hide-cancel didn't fire");
        }
        if (!hidden && seenDelta <= 0) {
            throw new IllegalStateException(
                    "[smoke/fcgt] ET sweep " + label + ": ET drew no trim through our wrap (seenDelta "
                            + seenDelta + ") at opacity " + opacity + " - the ET submit mixin isn't bound,"
                            + " or the trimmed elytra didn't render");
        }
        boolean fadeExpected = PARTIAL_FADE_SUPPORTED && !hidden && opacity < 1.0;
        if (fadeExpected && fadeDelta <= 0) {
            throw new IllegalStateException(
                    "[smoke/fcgt] ET sweep " + label + ": ET trim was not faded at opacity " + opacity
                            + " (fadeDelta " + fadeDelta + ") - the ELYTRA scope fade path didn't run");
        }
        if (!fadeExpected && fadeDelta != 0) {
            throw new IllegalStateException(
                    "[smoke/fcgt] ET sweep " + label + ": ET trim was faded at opacity " + opacity
                            + " (fadeDelta " + fadeDelta + ") where no fade may happen - at 100% the trim"
                            + " must render untouched, at 0% it must not render at all, and on cutout"
                            + " versions partial opacity is a no-op");
        }
    }

    /**
     * Fails if {@code shot} carries the ElytraTrims outline-tint signature - see
     * {@link ElytraTrimsSmokeTest} for the full description of the defect and why counting the
     * {@code R == 0 && G == 0 && B > 0} family over the whole frame is safe.
     *
     * @param shot  the screenshot to scan
     * @param label which sweep step this is, for the failure message
     */
    private static void assertNoOutlineTint(Path shot, String label) {
        int tinted = ScreenshotPixels.countMatching(shot, rgb -> ScreenshotPixels.red(rgb) == 0
                && ScreenshotPixels.green(rgb) == 0
                && ScreenshotPixels.blue(rgb) >= OUTLINE_TINT_MIN_BLUE);
        ArmorHider.LOGGER.info("[smoke/fcgt] ET sweep outline-tint scan at {}: {} pixels (tolerance {}) in {}",
                label, tinted, OUTLINE_TINT_TOLERANCE, shot);
        if (tinted > OUTLINE_TINT_TOLERANCE) {
            throw new IllegalStateException(
                    "[smoke/fcgt] ET outline-tint defect at " + label + ": " + tinted
                            + " pure-blue pixels (R==0 && G==0 && B>=" + OUTLINE_TINT_MIN_BLUE
                            + ") exceed the tolerance of " + OUTLINE_TINT_TOLERANCE + " in " + shot
                            + " - the trim was drawn as a full-bright outline tinted ARGB(0,0,0,100)"
                            + " instead of its normal colours.");
        }
    }

    /**
     * Deletes this test's screenshots from prior runs so each run leaves only its own artifacts. FCGT
     * reuses its {@code NNNN_} sequence prefix across runs, so a stale PNG could otherwise be scanned by
     * mistake; we match on the {@code armorhider_et_} basename, as the neighbouring ET smoke does.
     */
    private static void clearOldScreenshots() {
        try {
            Path dir = Path.of("screenshots");
            if (!Files.isDirectory(dir)) {
                return;
            }
            try (var stream = Files.newDirectoryStream(dir, "*armorhider_et_*.png")) {
                for (Path p : stream) {
                    Files.deleteIfExists(p);
                }
            }
        } catch (IOException e) {
            ArmorHider.LOGGER.warn("[smoke/fcgt] could not clear old ElytraTrims sweep screenshots", e);
        }
    }

    private static void snapPose(Minecraft client) {
        var player = client.player;
        if (player == null) {
            return;
        }
        player.setPos(0.5, PLAYER_Y, 0.5);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.setYRot(0.0F);
        player.setXRot(CAMERA_PITCH);
        player.setYHeadRot(0.0F);
        player.setYBodyRot(0.0F);
        player.getAbilities().flying = true;
        player.setNoGravity(true);
    }
}
//?}
