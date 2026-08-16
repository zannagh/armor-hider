// Reproduces the Iris "see-through body under faded armor" artifact (#342 follow-up). Needs a real
// GPU + Iris shaderpack, so it is only meaningful on a dev machine (the headless CI box uses software
// GL and cannot load shaders). Gated to the same FCGT + render-architecture floor as the water smoke.
//? if fcgt && >= 26.2-1.pre {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

/**
 * Repro harness for the Iris translucent-body bug: with a semi-transparent chestplate (opacity in
 * (0,1)) and an active shaderpack, the player torso reads see-through where the faded armor overlaps
 * it, so the sky / terrain behind draws through the body. The user reproduces this on 26.2 with
 * Complementary Reimagined, chest opacity 0.5, looking near-horizontally at a bright horizon.
 * <p>
 * This test recreates that framing so a fix can be eyeballed: player hovering high (only sky behind
 * at a near-horizontal view), a distinct stone pillar planted behind the torso as an occlusion probe,
 * noon so the sky is bright, third-person-back camera. It relies on the run dir's Iris + shaderpack
 * being loaded (the fabric-26.2 run/ has ComplementaryReimagined selected in iris.properties).
 * <p>
 * Not a machine-checked assertion test - it exists to produce before/after screenshots on a real GPU.
 */
public final class IrisTranslucencySmokeTest implements FabricClientGameTest {

    // Near-horizontal so the bright sky fills the background behind the torso (the reported framing).
    private static final float CAMERA_PITCH = 2.0F;
    private static final double PLAYER_Y = 200.0;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Iris translucency repro starting");
        context.waitForScreen(TitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {
            ArmorHider.LOGGER.info("[smoke/fcgt] World created");

            var server = singleplayer.getServer();
            server.runOnServer(mcServer -> {
                var level = mcServer.overworld();
                // A fresh consistent-settings world starts at time 0 (sunrise) - sun on the horizon
                // with a bright sky, which is exactly the reported condition, so we don't touch time.
                buildProbe(level);
                var sp = mcServer.getPlayerList().getPlayers().get(0);
                var abilities = sp.getAbilities();
                abilities.mayfly = true;
                abilities.flying = true;
                sp.onUpdateAbilities();
                sp.setNoGravity(true);
                sp.connection.teleport(0.5, PLAYER_Y, 0.5, 0.0F, CAMERA_PITCH);
            });

            context.runOnClient(client -> {
                var player = client.player;
                if (player == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
                }
                var chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                // Force the enchantment glint (the reported case had an enchanted chestplate) so the
                // glint-on-faded-armor path is exercised under shaders without needing a registry lookup.
                chest.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
                player.setItemSlot(EquipmentSlot.CHEST, chest);
                player.setYRot(0.0F);
                player.setXRot(CAMERA_PITCH);
                player.setYHeadRot(0.0F);
                player.getAbilities().flying = true;
                player.setNoGravity(true);
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                config.chestOpacity.setValue(0.5);
            });

            //? if >= 26.3-0.snapshot.8 {
            /*singleplayer.getConnection().waitForChunksRender();
            singleplayer.getConnection().waitForChunksRender();
            *///?} else {
            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getClientLevel().waitForChunksRender();
            //?}
            // Give Iris time to compile + warm the shaderpack before the first shot.
            context.waitTicks(120);

            boolean shaders = context.computeOnClient(client -> ArmorHiderRenderTypes.isShaderPackActive());
            ArmorHider.LOGGER.info("[smoke/fcgt] shaderPackActive reported by IrisApi = {}", shaders);

            // Force the shaderpack-active override so the under-shaders dither path is exercised
            // deterministically, whether or not a real shaderpack is loaded (the headless CI box has no
            // GPU shaderpack; a dev machine with the run/ Iris pack renders the real thing for eyeballing).
            context.runOnClient(client -> ArmorHiderRenderTypes.setShaderPackActiveOverride(Boolean.TRUE));

            // Production path at several opacities - each must route the faded chest onto the dithered
            // opaque cutout (never the translucent pass), so the torso stays solid with no see-through.
            // Machine-checked: the dither-path counter climbs at every opacity (guards the swap wiring,
            // which would otherwise fail silently on a target drift). The screenshots are for eyeballing
            // on a real GPU.
            double[] opacities = {0.25, 0.5, 0.75};
            for (double opacity : opacities) {
                long ditherBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.armorDitherPathCount());
                context.runOnClient(client -> {
                    var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                            .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                    config.chestOpacity.setValue(opacity);
                    snapPose(client);
                });
                context.waitTicks(15);
                String tag = "armorhider_iris_op" + (int) Math.round(opacity * 100);
                Path shot = context.takeScreenshot(tag);
                long ditherAfter = context.computeOnClient(client -> ArmorHiderRenderTypes.armorDitherPathCount());
                ArmorHider.LOGGER.info("[smoke/fcgt] Iris opacity {} ditherDelta={} screenshot: {}",
                        opacity, ditherAfter - ditherBefore, shot);
                if (ditherAfter - ditherBefore <= 0) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] faded chest at opacity " + opacity + " did not take the dithered "
                                    + "opaque-cutout path under shaders (delta 0) - the under-shaders dither "
                                    + "swap is not wired");
                }
            }

            // Fully-opaque control (opacity 1.0) - the torso must be solid there.
            context.runOnClient(client -> {
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                config.chestOpacity.setValue(1.0);
                snapPose(client);
            });
            context.waitTicks(20);
            Path opaqueShot = context.takeScreenshot("armorhider_iris_control_opaque");
            ArmorHider.LOGGER.info("[smoke/fcgt] Iris control (opaque) screenshot: {}", opaqueShot);

            context.runOnClient(client -> ArmorHiderRenderTypes.setShaderPackActiveOverride(null));
            ArmorHider.LOGGER.info("[smoke/fcgt] Iris translucency repro complete");
        }
    }

    // A stone pillar planted a few blocks behind the hovering player, spanning the torso's height, as
    // a hard occlusion probe: if the body reads see-through, the pillar (or sky) shows through it.
    private static void buildProbe(ServerLevel level) {
        BlockState air = Blocks.AIR.defaultBlockState();
        // A dark, unambiguously opaque occluder behind the torso: if the body reads see-through the
        // obsidian (or the bright sky beside it) shows through the player.
        BlockState probe = Blocks.OBSIDIAN.defaultBlockState();
        fill(level, -6, (int) PLAYER_Y - 6, -6, 6, (int) PLAYER_Y + 8, 10, air);
        // Pillar behind the player (+Z is where the third-person-back camera looks), 2 blocks wide.
        fill(level, -1, (int) PLAYER_Y - 2, 6, 0, (int) PLAYER_Y + 3, 6, probe);
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

    private static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    level.setBlock(pos.set(x, y, z), state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
//?}
