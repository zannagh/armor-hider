// Reproduces issue #324: the enchantment glint vanishes on a semi-transparent (faded) armor piece
// instead of fading with it. Gated the same way as the water-transparency smoke - the FCGT API this
// compiles against and the render architecture it drives both line up at >= 26.2-1.pre.
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

/**
 * Phase glint-transparency render smoke (fabric-client-gametest-api-v1).
 * <p>
 * Covers the faded-enchanted-armor behaviour after the issue #324 co-draw was reverted. The mod no
 * longer re-issues the enchantment glint onto a translucent piece: on a faded (depth-write-disabled)
 * base the vanilla glint's EQUAL depth test fails and the glint vanishes with the fade, which is the
 * intended behaviour (the co-draw painted the whole model, mismatched modded/texture-pack armor
 * outlines and broke under shaders). What must still hold is that a faded enchanted piece keeps
 * <em>fading</em>: on 26.3 the fused armor+glint shader clamps output alpha up to the Glint Strength
 * setting, so before the revert an enchanted piece rendered ~opaque even at 5% (#3) and vanished under
 * "Improved Transparency"/OIT (#4). Routing the faded enchanted piece onto the plain translucent armor
 * type (which fades and carries OIT) fixes both, at the cost of the glint on faded armor.
 * <p>
 * The scene is a hovering player in third-person-back against a solid stone backdrop (a plain, matte
 * background makes the iridescent glint easy to read), wearing an enchanted netherite chestplate. Glint
 * is forced on via {@link DataComponents#ENCHANTMENT_GLINT_OVERRIDE} so the test needs no enchantment
 * registry lookup and behaves identically on every version.
 * <p>
 * Three shots are captured for eyeballing (FCGT does no pixel diffing):
 * <ol>
 *   <li>{@code 100} - fully opaque enchanted chest: the glint positive control (mod is a no-op here,
 *       so this is vanilla-equivalent and must always show a full glint).</li>
 *   <li>{@code 40} - the reported case: a mostly-faded chest. The chest must be translucent (fading,
 *       not opaque); the glint is gone.</li>
 *   <li>{@code 05} - nearly hidden: the chest is barely-there and still fading, not popped back to
 *       opaque by an enchant glint.</li>
 * </ol>
 * A machine check asserts the faded enchanted piece is routed onto the translucent armor type (so it
 * fades). Gated to the {@code fcgt} constant, currently wired for fabric-26.2.
 */
public final class GlintTransparencySmokeTest implements FabricClientGameTest {

    private static final float CAMERA_PITCH = 8.0F;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Glint transparency smoke starting");
        context.waitForScreen(TitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {
            ArmorHider.LOGGER.info("[smoke/fcgt] World created, building stone backdrop arena");

            var server = singleplayer.getServer();
            server.runOnServer(mcServer -> {
                buildArena(mcServer.overworld(), Blocks.STONE.defaultBlockState());
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
                // Netherite: a dark chestplate so the violet glint reads clearly against it under
                // software GL. The enchant/plain toggle below flips only the glint override, so an
                // image diff of the pair isolates the glint contribution exactly.
                player.setItemSlot(EquipmentSlot.CHEST, chestplate(true));

                player.setYRot(0.0F);
                player.setXRot(CAMERA_PITCH);
                player.setYHeadRot(0.0F);
                player.getAbilities().flying = true;
                player.setNoGravity(true);
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                // Keep the glint (affect-glint on / do NOT disable it) and start fully opaque.
                config.chestGlint.setValue(Boolean.TRUE);
                config.chestOpacity.setValue(1.0);
            });

            //? if >= 26.3-0.snapshot.8 {
            /*singleplayer.getConnection().waitForChunksRender();
            singleplayer.getConnection().waitForChunksRender();
            *///?} else {
            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getClientLevel().waitForChunksRender();
            //?}
            context.waitTicks(40);

            // 100% is the positive control: the mod is a no-op there (needsModification false), so this
            // is vanilla-equivalent and must always show a full glint. Captured enchanted + plain so a
            // diff isolates the glint regardless of how subtle it reads under llvmpipe.
            captureGlintPair(context, 1.0, "100");

            // The reported #3/#4 case: a mostly-faded enchanted chest. The mod no longer forces the
            // glint onto a translucent co-draw type - on a faded (depth-write-disabled) base the vanilla
            // glint's EQUAL depth test fails and the glint simply vanishes with the fade, which is the
            // intended behaviour. But the piece MUST still fade: pre-revert on 26.3 the fused glint
            // shader clamped output alpha back up to the Glint Strength setting (color.a = max(color.a,
            // GlintAlpha)) so an enchanted piece rendered ~opaque even at 5% opacity (#3), and its OIT
            // pipeline was wrong so it vanished under "Improved Transparency" (#4). Both are fixed by
            // routing the faded enchanted piece onto the plain translucent armor type (fades + carries
            // OIT), dropping the glint. Assert that routing actually happens.
            long routedBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.armorNoDepthPathCount());
            captureGlintPair(context, 0.4, "40");
            captureGlintPair(context, 0.05, "05");
            long routedAfter = context.computeOnClient(client -> ArmorHiderRenderTypes.armorNoDepthPathCount());

            // Machine check (per this repo's smoke convention): the faded enchanted piece must be routed
            // onto our translucent armor type so it fades. If the armorCutoutNoCullGlint/armorCutoutNoCull
            // swap silently misses its target on a version bump, the piece would render opaque (the #3
            // regression) with no crash to notice.
            if (routedAfter <= routedBefore) {
                throw new IllegalStateException(
                        "[smoke/fcgt] faded enchanted armor was never routed onto the translucent armor type "
                                + "(delta " + (routedAfter - routedBefore) + ") - it would render opaque instead "
                                + "of fading; the armor render-type swap missed its target");
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] faded-armor translucent routings during capture: {}", routedAfter - routedBefore);

            ArmorHider.LOGGER.info("[smoke/fcgt] Glint transparency smoke complete");
        }
    }

    // A netherite chestplate, optionally forced to show the enchantment glint (no registry lookup).
    private static ItemStack chestplate(boolean enchanted) {
        ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
        chest.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, enchanted ? Boolean.TRUE : Boolean.FALSE);
        return chest;
    }

    // Captures an enchanted/plain pair at the given chest opacity. The plain shot is the exact same
    // scene with the glint override off, so diff(enchanted, plain) is the glint alone.
    private static void captureGlintPair(ClientGameTestContext context, double opacity, String label) {
        context.runOnClient(client -> {
            var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
            config.chestOpacity.setValue(opacity);
            if (client.player != null) {
                client.player.setItemSlot(EquipmentSlot.CHEST, chestplate(true));
            }
            snapPose(client);
        });
        context.waitTicks(6);
        Path ench = context.takeScreenshot("armorhider_glint_" + label + "_1_ench");
        ArmorHider.LOGGER.info("[smoke/fcgt] glint {}% enchanted screenshot: {}", label, ench);

        context.runOnClient(client -> {
            if (client.player != null) {
                client.player.setItemSlot(EquipmentSlot.CHEST, chestplate(false));
            }
            snapPose(client);
        });
        context.waitTicks(6);
        Path plain = context.takeScreenshot("armorhider_glint_" + label + "_2_plain");
        ArmorHider.LOGGER.info("[smoke/fcgt] glint {}% plain screenshot: {}", label, plain);
    }

    private static void buildArena(ServerLevel level, BlockState wall) {
        BlockState air = Blocks.AIR.defaultBlockState();
        fill(level, -20, 66, -6, 20, 128, 22, air);
        fill(level, -18, 70, 8, 18, 116, 9, wall);
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
