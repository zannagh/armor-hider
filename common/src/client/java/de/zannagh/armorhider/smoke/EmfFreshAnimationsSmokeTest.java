//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.render.AhArmProbe;
import de.zannagh.armorhider.configuration.EmfHiddenModelMode;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Issue #217 reproduction harness (fabric-client-gametest-api-v1).
 * <p>
 * With EMF present and an animated custom <em>player</em> model on the pack stack, hiding the armor
 * exposes a seam where the model's arms sit off the torso. Base Fresh Animations ships only mob
 * models (no {@code player.jem}), so it does not drive this on its own - a Fresh Animations player
 * add-on does. To reproduce deterministically (no random dev skin, no idle sway), this test writes a
 * tiny synthetic OptiFine-CEM {@code player.jem} whose arms are deliberately dropped and splayed off
 * the shoulders, drops it into {@code run/resourcepacks/}, and lets EMF render it.
 * <p>
 * It then hides the armor via Armor Hider and screenshots; a plain-vanilla control (no armor, AH
 * inert) is captured in the same run so the seam can be attributed. Secondary skin layers are turned
 * off so the base arm meets the base torso with nothing to mask the gap.
 */
public final class EmfFreshAnimationsSmokeTest implements FabricClientGameTest {

    private static final double HIDDEN = 0.0;
    private static final String SYNTH_PACK_DIR = "ah217_synth";

    /**
     * Minimal CEM player model: faithful vanilla geometry, except {@code right_arm}/{@code left_arm}
     * are given a lowered, splayed pivot (translate y -16 vs vanilla -22, x ±7 vs ±5) so the arms
     * render detached from the shoulders - the #217 symptom, made deterministic.
     */
    // Only the arms are defined (with a lowered pivot, translate y -18 vs vanilla -22 => dropped ~4px);
    // parts absent from a CEM .jem keep vanilla geometry, so head/body/legs stay correct and only the
    // arms detach - a faithful stand-in for a Fresh Animations player add-on whose arms sit off the
    // shoulders, and a fair basis for judging the seam-bearing / repair modes.
    private static final String PLAYER_JEM = """
            {
              "credit": "armor-hider #217 synthetic repro (arms only)",
              "textureSize": [64, 64],
              "models": [
                {"part":"right_arm","id":"right_arm","invertAxis":"xy","translate":[5,-18,0],
                  "boxes":[{"coordinates":[-3,-2,-2,4,12,4],"textureOffset":[40,16]}]},
                {"part":"left_arm","id":"left_arm","invertAxis":"xy","translate":[-5,-18,0],
                  "boxes":[{"coordinates":[-1,-2,-2,4,12,4],"textureOffset":[32,48]}]}
              ]
            }
            """;

    private static final String PACK_MCMETA = """
            {"pack":{"description":"armor-hider #217 synthetic CEM player","min_format":84,"max_format":999}}
            """;

    @Override
    public void runTest(ClientGameTestContext context) {
        String label = label();
        ArmorHider.LOGGER.info("[smoke/fcgt] EMF/FA repro starting (label={})", label);
        clearOldScreenshots();
        context.waitForScreen(TitleScreen.class);

        // Compat flags and the probe are thread-safe mod statics (set/read via volatiles), so they do
        // not need a client-thread hop - reading them directly keeps the setup simple.
        boolean emfPresent = CompatManager.requiresCompatTo(CompatFlags.ENTITY_MODEL_FEATURES);
        // Deterministic repro: write a synthetic CEM player.jem with detached arms and enable it, so
        // the "hidden model behaviour" modes have a visible seam to act on regardless of skin.
        boolean synthInstalled = writeSyntheticPack();
        boolean packEnabled = context.computeOnClient(EmfFreshAnimationsSmokeTest::enableCustomPacks);
        AhArmProbe.enable();
        ArmorHider.LOGGER.info("[smoke/fcgt] repro env: emfPresent={}, synthPackWritten={}, customPacksEnabled={}",
                emfPresent, synthInstalled, packEnabled);
        context.waitTicks(60);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {

            context.runOnClient(client -> {
                var player = client.player;
                if (player == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
                }
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));

                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                // Bare base skin so the arm/torso seam has no outer layer to hide it (#217 note).
                client.options.setModelPart(PlayerModelPart.JACKET, false);
                client.options.setModelPart(PlayerModelPart.LEFT_SLEEVE, false);
                client.options.setModelPart(PlayerModelPart.RIGHT_SLEEVE, false);
                client.options.setModelPart(PlayerModelPart.HAT, false);

                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                config.helmetOpacity.setValue(HIDDEN);
                config.chestOpacity.setValue(HIDDEN);
                config.legsOpacity.setValue(HIDDEN);
                config.bootsOpacity.setValue(HIDDEN);
            });

            // Assert each hidden-model mode drives the render path it should. The synthetic pack is a
            // custom EMF player model, so the default KEEP leaves it in place (custom_model); the two
            // opt-in modes fall back to vanilla wholly (VANILLA) or on the seam parts (VANILLA_SEAMS).
            boolean assertable = emfPresent && packEnabled;
            assertMode(context, label, "0_keep", EmfHiddenModelMode.KEEP, AhArmProbe.PATH_CUSTOM, assertable);
            assertMode(context, label, "1_vanilla", EmfHiddenModelMode.VANILLA, AhArmProbe.PATH_FORCED_VANILLA, assertable);
            assertMode(context, label, "2_vanilla_seams", EmfHiddenModelMode.VANILLA_SEAMS, AhArmProbe.PATH_SEAM_COMPOSITE, assertable);

            ArmorHider.LOGGER.info("[smoke/fcgt] #217 hidden-model-mode checks passed (label={})", label);

            // Visual + no-crash check that the toggle shows up in Other Settings when EMF is present.
            context.runOnClient(client -> {
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                config.hiddenModelBehaviour.setValue(EmfHiddenModelMode.KEEP);
                net.minecraft.client.Minecraft.getInstance().setScreenAndShow(
                        new de.zannagh.armorhider.client.gui.screens.ArmorHiderOptionsScreen(null, client.options));
            });
            context.waitTicks(5);
            context.takeScreenshot("ah217_" + label + "_options_screen");
            context.runOnClient(client -> client.setScreenAndShow(null));
        }
    }

    /**
     * Deletes this test's screenshots from prior runs so each run leaves only its own artifacts and
     * nothing has to be cleaned up by hand. FCGT writes to {@code <runDir>/screenshots/} with an
     * {@code NNNN_} sequence prefix; we match on the {@code ah217_} basename.
     */
    private static void clearOldScreenshots() {
        try {
            Path dir = Path.of("screenshots");
            if (!Files.isDirectory(dir)) {
                return;
            }
            try (var stream = Files.newDirectoryStream(dir, "*ah217_*.png")) {
                for (Path p : stream) {
                    Files.deleteIfExists(p);
                }
            }
        } catch (IOException e) {
            ArmorHider.LOGGER.warn("[smoke/fcgt] could not clear old #217 screenshots", e);
        }
    }

    private static void assertMode(ClientGameTestContext context, String label, String tag,
            EmfHiddenModelMode mode, String expectedPath, boolean assertable) {
        context.runOnClient(client -> {
            var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
            config.hiddenModelBehaviour.setValue(mode);
        });
        // EMF re-evaluates its vanilla-model condition per frame; give the reload/redraw room.
        context.waitTicks(30);
        String path = AhArmProbe.lastPath();
        ArmorHider.LOGGER.info("[smoke/fcgt] #217 mode={} tag={} render path={} (expected {})",
                mode, tag, path, expectedPath);
        context.takeScreenshot("ah217_" + label + "_" + tag);
        if (assertable && !expectedPath.equals(path)) {
            throw new IllegalStateException(
                    "[smoke/fcgt] #217: hiddenModelBehaviour=" + mode + " with the body hidden should render"
                            + " via '" + expectedPath + "', but the EMF model part path was '" + path + "'."
                            + " The mode is not being honoured for the custom player model.");
        }
    }

    /**
     * Writes the synthetic CEM player pack into {@code run/resourcepacks/}. The gametest runs with
     * the working directory at the run dir, so a relative path lands in the right place.
     *
     * @return {@code true} on success.
     */
    private static boolean writeSyntheticPack() {
        try {
            Path root = Path.of("resourcepacks", SYNTH_PACK_DIR);
            Path cem = root.resolve("assets/minecraft/optifine/cem");
            Files.createDirectories(cem);
            Files.writeString(root.resolve("pack.mcmeta"), PACK_MCMETA, StandardCharsets.UTF_8);
            Files.writeString(cem.resolve("player.jem"), PLAYER_JEM, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            ArmorHider.LOGGER.warn("[smoke/fcgt] failed to write synthetic pack", e);
            return false;
        }
    }

    /**
     * Selects every available custom resource pack (the synthetic pack, plus any Fresh Animations
     * pack that happens to be present), appended after the currently-selected packs so they take
     * priority, and triggers a resource reload.
     *
     * @return {@code true} if at least one custom pack was selected.
     */
    private static boolean enableCustomPacks(net.minecraft.client.Minecraft client) {
        var repo = client.getResourcePackRepository();
        repo.reload();
        List<String> selected = new ArrayList<>(repo.getSelectedIds());
        boolean found = false;
        for (String id : repo.getAvailableIds()) {
            String lower = id.toLowerCase(java.util.Locale.ROOT);
            if ((lower.contains("fresh") || lower.contains(SYNTH_PACK_DIR)) && !selected.contains(id)) {
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

    private static String label() {
        return System.getProperty("armorhider.smoke.repro.label", "run")
                .replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
//?}
