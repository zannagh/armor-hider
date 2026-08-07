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
    private static final String PLAYER_JEM = """
            {
              "credit": "armor-hider #217 synthetic repro",
              "textureSize": [64, 64],
              "models": [
                {"part":"head","id":"head","invertAxis":"xy","translate":[0,-24,0],
                  "boxes":[{"coordinates":[-4,-8,-4,8,8,8],"textureOffset":[0,0]}]},
                {"part":"body","id":"body","invertAxis":"xy","translate":[0,-24,0],
                  "boxes":[{"coordinates":[-4,0,-2,8,12,4],"textureOffset":[16,16]}]},
                {"part":"right_arm","id":"right_arm","invertAxis":"xy","translate":[7,-16,0],
                  "boxes":[{"coordinates":[-3,-2,-2,4,12,4],"textureOffset":[40,16]}]},
                {"part":"left_arm","id":"left_arm","invertAxis":"xy","translate":[-7,-16,0],
                  "boxes":[{"coordinates":[-1,-2,-2,4,12,4],"textureOffset":[32,48]}]},
                {"part":"right_leg","id":"right_leg","invertAxis":"xy","translate":[1.9,-12,0],
                  "boxes":[{"coordinates":[-2,0,-2,4,12,4],"textureOffset":[0,16]}]},
                {"part":"left_leg","id":"left_leg","invertAxis":"xy","translate":[-1.9,-12,0],
                  "boxes":[{"coordinates":[-2,0,-2,4,12,4],"textureOffset":[16,48]}]}
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
        context.waitForScreen(TitleScreen.class);

        boolean emfPresent = context.computeOnClient(client ->
                CompatManager.requiresCompatTo(CompatFlags.ENTITY_MODEL_FEATURES));
        boolean synthInstalled = writeSyntheticPack();
        boolean packEnabled = context.computeOnClient(EmfFreshAnimationsSmokeTest::enableCustomPacks);
        context.runOnClient(client -> AhArmProbe.enable());
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

            context.waitTicks(40);
            context.takeScreenshot("ah217_" + label + "_1_hidden_front");

            // Assert the fix: with the body hidden and a custom EMF player model present, EMF must be
            // forced to the vanilla model (which closes the arm/torso seam). Without the fix this stays
            // on the custom model and the seam shows - exactly #217.
            String pathHidden = context.computeOnClient(client -> AhArmProbe.lastPath());
            ArmorHider.LOGGER.info("[smoke/fcgt] #217 probe (chest hidden): render path = {}", pathHidden);
            if (emfPresent && packEnabled) {
                if (!AhArmProbe.PATH_FORCED_VANILLA.equals(pathHidden)) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] #217: body hidden with a custom EMF player model, but EMF was not"
                                    + " forced to the vanilla model (render path=" + pathHidden + "). The custom"
                                    + " model's arm/torso seam is left exposed - EmfCompat#bodyRegionHidden did"
                                    + " not trigger the vanilla fallback.");
                }
            } else {
                ArmorHider.LOGGER.warn("[smoke/fcgt] #217 assertion skipped: emfPresent={}, packEnabled={}"
                        + " (run with -Pcompat=emf,etf to exercise the fix)", emfPresent, packEnabled);
            }

            // Control: strip all armor and restore full opacity so Armor Hider does nothing. If the
            // arms are still detached here it is the custom player model's doing (AH merely exposed
            // it); if they re-attach, AH is actively perturbing the model.
            context.runOnClient(client -> {
                var player = client.player;
                if (player != null) {
                    player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                    player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                    player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                    var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                            .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                    config.helmetOpacity.setValue(1.0);
                    config.chestOpacity.setValue(1.0);
                    config.legsOpacity.setValue(1.0);
                    config.bootsOpacity.setValue(1.0);
                }
            });
            context.waitTicks(40);
            context.takeScreenshot("ah217_" + label + "_2_noarmor_front");

            ArmorHider.LOGGER.info("[smoke/fcgt] EMF/FA repro screenshots captured (label={})", label);
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
