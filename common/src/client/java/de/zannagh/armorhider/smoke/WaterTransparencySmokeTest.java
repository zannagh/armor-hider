// Drives the after-terrain feature phase, which only exists >= 26.2-1.pre; older fcgt variants
// (1.21.x, 26.1.x, 26.2 snapshots) use a different render architecture and also predate the FCGT
// API this test compiles against, so gate the whole class to where both line up.
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
 * Phase 3 water-transparency render smoke (fabric-client-gametest-api-v1).
 * <p>
 * Reproduces the reported artifact: a semi-transparent chestplate's shoulder pads (pauldrons)
 * vanish when silhouetted against a body of water, because the translucent-no-depth-write armor
 * was drawn in the pre-terrain {@code translucentModels} phase and the translucent water terrain,
 * drawn afterwards, overdrew the pad pixels not backed by an opaque body pixel. The fix routes our
 * translucent armor into the vanilla {@code afterTerrain} feature phase (drawn after the
 * translucent terrain layer) so the pads blend over the already-present water instead.
 * <p>
 * The scene is built entirely with {@link ServerLevel#setBlock} (no {@code /fill} commands, so no
 * command feedback clutters the chat overlay): a tall, wide backdrop wall a few blocks in front of
 * a hovering player, from a near-horizontal third-person-back camera — so the pauldrons that
 * protrude past the body silhouette are seen against the wall with no opaque body pixel behind them.
 * The wall is placed with {@code UPDATE_CLIENTS} (no neighbour updates), which keeps the water as
 * non-flowing source blocks — a stable vertical water backdrop vanilla fluid physics would drain.
 * <p>
 * Three shots are captured from an identical, snapped pose for eyeballing (FCGT does no pixel diffing
 * here): the redirect toggled off (pre-fix — water overdraws the pads), toggled on (fixed), and a
 * stone-backdrop control. The machine-checked assertions are that the deferral stays quiet while the
 * redirect is off ({@link ArmorHiderRenderTypes#deferredSubmitCount()} flat) and fires while it is on
 * — guarding, per this repo's smoke-test convention, against the mixin silently missing its target on
 * a version bump, which would revert the piece to the old overdraw behaviour with no crash to notice.
 * <p>
 * Gated to the {@code fcgt} constant and, via the render architecture it drives, effectively to
 * {@code >= 26.2-1.pre} where the feature-phase {@code afterTerrain} model phase exists. Currently
 * wired for fabric-26.2.
 */
public final class WaterTransparencySmokeTest implements FabricClientGameTest {

    // Near-horizontal back view (matches the reported third-person scenario): the pauldrons that
    // protrude sideways past the body silhouette are seen against the water backdrop behind the
    // player, with no opaque body pixel behind them — exactly the pixels the bug drops.
    private static final float CAMERA_PITCH = 8.0F;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Water transparency smoke starting");
        context.waitForScreen(TitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {
            ArmorHider.LOGGER.info("[smoke/fcgt] World created, building water pool arena");

            var server = singleplayer.getServer();
            server.runOnServer(mcServer -> {
                buildArena(mcServer.overworld(), Blocks.WATER.defaultBlockState());
                var sp = mcServer.getPlayerList().getPlayers().get(0);
                // Hover the player (creative flight, no gravity) so nothing opaque sits behind the
                // sideways pauldrons — only the water wall does. Facing +Z (yaw 0) toward the wall.
                var abilities = sp.getAbilities();
                abilities.mayfly = true;
                abilities.flying = true;
                sp.onUpdateAbilities();
                sp.setNoGravity(true);
                sp.connection.teleport(0.5, 96.0, 0.5, 0.0F, CAMERA_PITCH);
            });

            context.runOnClient(client -> {
                var player = client.player;
                if (player == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
                }
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                // Reassert rotation + flight client-side so the framing is deterministic.
                player.setYRot(0.0F);
                player.setXRot(CAMERA_PITCH);
                player.setYHeadRot(0.0F);
                player.getAbilities().flying = true;
                player.setNoGravity(true);
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

                // A prior smoke test (keybind) flips the session disable override on and clears it again;
                // clear it defensively so the chest actually fades here regardless of test ordering.
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                // 80% chest opacity → the reported case: mostly-opaque pads that must survive over water.
                config.chestOpacity.setValue(0.8);

                // Start with the redirect OFF to capture the pre-fix "before": pads overdrawn by water.
                ArmorHiderRenderTypes.setDeferralEnabled(false);
            });

            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getClientLevel().waitForChunksRender();
            context.waitTicks(40);

            // BEFORE — redirect off: the faded armor is drawn in the pre-terrain phase, so the water
            // overdraws the pads that aren't backed by an opaque body pixel (the reported bug).
            context.runOnClient(WaterTransparencySmokeTest::snapPose);
            context.waitTicks(3);
            long brokenStart = context.computeOnClient(client -> ArmorHiderRenderTypes.deferredSubmitCount());
            Path brokenShot = context.takeScreenshot("armorhider_1_water_redirect_off");
            long brokenEnd = context.computeOnClient(client -> ArmorHiderRenderTypes.deferredSubmitCount());
            ArmorHider.LOGGER.info("[smoke/fcgt] BEFORE (redirect off) screenshot: {}", brokenShot);

            // AFTER — redirect on: the faded armor draws in the after-terrain phase, over the water.
            // Same pose (snapped again) so the before/after is a clean pixel-comparable pair.
            context.runOnClient(client -> {
                ArmorHiderRenderTypes.setDeferralEnabled(true);
                snapPose(client);
            });
            context.waitTicks(6);
            long fixedStart = context.computeOnClient(client -> ArmorHiderRenderTypes.deferredSubmitCount());
            Path fixedShot = context.takeScreenshot("armorhider_2_water_redirect_on");
            long fixedEnd = context.computeOnClient(client -> ArmorHiderRenderTypes.deferredSubmitCount());
            ArmorHider.LOGGER.info("[smoke/fcgt] AFTER (redirect on) screenshot: {}", fixedShot);

            // Control — same pose, redirect on, but background swapped to solid stone. Pre-fix this
            // rendered the pads fine; it should look the same as the water AFTER shot.
            server.runOnServer(mcServer -> buildArena(mcServer.overworld(), Blocks.STONE.defaultBlockState()));
            singleplayer.getClientLevel().waitForChunksRender();
            context.runOnClient(WaterTransparencySmokeTest::snapPose);
            context.waitTicks(6);
            Path stoneShot = context.takeScreenshot("armorhider_3_solid_redirect_on");
            ArmorHider.LOGGER.info("[smoke/fcgt] CONTROL (solid background) screenshot: {}", stoneShot);

            long deltaBroken = brokenEnd - brokenStart;
            long deltaFixed = fixedEnd - fixedStart;
            if (deltaBroken != 0) {
                throw new IllegalStateException("[smoke/fcgt] redirect fired while disabled (delta "
                        + deltaBroken + ") — the deferral toggle is not honoured");
            }
            if (deltaFixed <= 0) {
                throw new IllegalStateException(
                        "[smoke/fcgt] after-terrain deferral never fired while enabled (delta " + deltaFixed
                                + ") — the translucent armor is still drawn before the water terrain;"
                                + " the redirect mixin missed its target");
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] deferrals: off-window={}, on-window={}", deltaBroken, deltaFixed);

            ArmorHider.LOGGER.info("[smoke/fcgt] Water transparency smoke complete");
        }
    }

    // Builds a tall, wide backdrop wall a few blocks in front of the hovering player (at +Z, the
    // side the third-person-back camera looks toward). {@code wall} is water for the reproduction,
    // stone for the comparison shot. UPDATE_CLIENTS syncs to the client without neighbour updates,
    // so the water is placed as non-flowing source blocks — a stable vertical water backdrop that
    // vanilla fluid physics would otherwise drain in a second.
    private static void buildArena(ServerLevel level, BlockState wall) {
        BlockState air = Blocks.AIR.defaultBlockState();
        fill(level, -20, 66, -6, 20, 128, 22, air);   // clear the arena around player + wall
        fill(level, -18, 70, 8, 18, 116, 9, wall);     // tall, wide backdrop wall at z=8..9
    }

    // Re-pin the player to an exact, motionless pose so every screenshot is framed identically
    // (the before/after pair must be pixel-comparable) and flight/no-gravity can't lapse mid-run.
    private static void snapPose(Minecraft client) {
        var player = client.player;
        if (player == null) {
            return;
        }
        player.setPos(0.5, 96.0, 0.5);
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
