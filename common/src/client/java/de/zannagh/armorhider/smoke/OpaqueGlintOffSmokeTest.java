// Regression for the Iris shader bleed-through: a FULLY OPAQUE armor piece whose glint is merely
// turned off must NOT be routed onto the depth-write-disabled translucent (deferred) pipeline - doing
// so makes it read as see-through under shaders against bright light or water even at 100% opacity.
// The mod's own after-terrain deferral counter is the shader-independent proxy: an opaque piece must
// not defer, a genuinely faded one must. Gated like the water/glint smokes (>= 26.2-1.pre).
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
 * Phase opaque-glint-off render smoke (fabric-client-gametest-api-v1).
 * <p>
 * Reproduces the reported Iris artifact: at 100% armor opacity the player/armor still bleed the
 * background through under a shaderpack. Root cause: {@code needsModification} was true whenever a
 * slot's glint was disabled - even at full opacity - which swapped the fully-opaque piece onto the
 * depth-write-disabled translucent pipeline and deferred it after terrain under Iris's
 * ENTITIES_TRANSLUCENT program. The fix gates the render-type swap on genuine translucency
 * ({@code needsTranslucency}, opacity &lt; ~1) instead, so glint-off no longer implies see-through.
 * <p>
 * Shader-independent check (this box has no Iris): the after-terrain deferral counter
 * ({@link ArmorHiderRenderTypes#deferredSubmitCount()}) is the proxy for "went on the no-depth
 * translucent path". A 100%-opaque, glint-off chest must NOT increment it; a genuinely faded chest
 * must. Two screenshots are captured for eyeballing (the opaque one must look solid).
 */
public final class OpaqueGlintOffSmokeTest implements FabricClientGameTest {

    private static final float CAMERA_PITCH = 8.0F;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Opaque glint-off smoke starting");
        context.waitForScreen(TitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {

            var server = singleplayer.getServer();
            server.runOnServer(mcServer -> {
                buildArena(mcServer.overworld());
                var sp = mcServer.getPlayerList().getPlayers().get(0);
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
                player.setYRot(0.0F);
                player.setXRot(CAMERA_PITCH);
                player.setYHeadRot(0.0F);
                player.getAbilities().flying = true;
                player.setNoGravity(true);
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                // The reported configuration: full opacity, but the chest's glint turned OFF.
                config.chestGlint.setValue(Boolean.FALSE);
                config.chestOpacity.setValue(1.0);
                ArmorHiderRenderTypes.setDeferralEnabled(true);
            });

            //? if >= 26.3-0.snapshot.8 {
            /*singleplayer.getConnection().waitForChunksRender();
            singleplayer.getConnection().waitForChunksRender();
            *///?} else {
            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getClientLevel().waitForChunksRender();
            //?}
            context.waitTicks(40);

            // 1) Fully opaque, glint off: must render solid and must NOT defer (no no-depth swap).
            context.runOnClient(OpaqueGlintOffSmokeTest::snapPose);
            context.waitTicks(3);
            long opaqueBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.deferredSubmitCount());
            context.waitTicks(3);
            Path opaqueShot = context.takeScreenshot("armorhider_opaque_1_glintoff_100");
            long opaqueAfter = context.computeOnClient(client -> ArmorHiderRenderTypes.deferredSubmitCount());
            ArmorHider.LOGGER.info("[smoke/fcgt] opaque glint-off screenshot: {} (defer delta {})",
                    opaqueShot, opaqueAfter - opaqueBefore);

            // 2) Positive control: fade the same chest to 40% - now it SHOULD defer.
            context.runOnClient(client -> {
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                config.chestOpacity.setValue(0.4);
                snapPose(client);
            });
            context.waitTicks(6);
            long fadedBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.deferredSubmitCount());
            context.waitTicks(3);
            Path fadedShot = context.takeScreenshot("armorhider_opaque_2_faded_40");
            long fadedAfter = context.computeOnClient(client -> ArmorHiderRenderTypes.deferredSubmitCount());
            ArmorHider.LOGGER.info("[smoke/fcgt] faded control screenshot: {} (defer delta {})",
                    fadedShot, fadedAfter - fadedBefore);

            long opaqueDelta = opaqueAfter - opaqueBefore;
            long fadedDelta = fadedAfter - fadedBefore;
            if (opaqueDelta != 0) {
                throw new IllegalStateException(
                        "[smoke/fcgt] a fully-opaque, glint-off chest was deferred onto the no-depth"
                                + " translucent path (delta " + opaqueDelta + ") - it will read as"
                                + " see-through under shaders at 100% opacity");
            }
            if (fadedDelta <= 0) {
                throw new IllegalStateException(
                        "[smoke/fcgt] a genuinely faded chest did not defer (delta " + fadedDelta
                                + ") - the translucency path regressed");
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] defer deltas: opaque-glint-off={}, faded={}",
                    opaqueDelta, fadedDelta);
            ArmorHider.LOGGER.info("[smoke/fcgt] Opaque glint-off smoke complete");
        }
    }

    private static void buildArena(ServerLevel level) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        fill(level, -20, 66, -6, 20, 128, 22, air);
        fill(level, -18, 70, 8, 18, 116, 9, stone);
    }

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
