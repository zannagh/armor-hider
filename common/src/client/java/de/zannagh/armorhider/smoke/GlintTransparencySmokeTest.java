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
 * Reproduces issue #324: with a slot's glint kept ("affect glint" on) and the piece faded to a partial
 * opacity, the enchantment glint disappears entirely rather than fading with the item. The mod's
 * translucent armor render type is built from the armor texture alone and carries no glint pass, so a
 * faded enchanted piece swapped onto it loses the glint. The expected behaviour is a glint that stays
 * visible but fades in step with the item's opacity.
 * <p>
 * The scene is a hovering player in third-person-back against a solid stone backdrop (a plain, matte
 * background makes the iridescent glint easy to read), wearing an enchanted diamond chestplate. Glint
 * is forced on via {@link DataComponents#ENCHANTMENT_GLINT_OVERRIDE} so the test needs no enchantment
 * registry lookup and behaves identically on every version.
 * <p>
 * Three shots are captured for eyeballing (FCGT does no pixel diffing):
 * <ol>
 *   <li>{@code 100_opacity} - fully opaque enchanted chest: the glint positive control (mod is a
 *       no-op here, so this is vanilla-equivalent and must always show glint).</li>
 *   <li>{@code 40_opacity} - the reported case: a mostly-faded chest that must still show a (faded)
 *       glint. Pre-fix this frame shows a translucent chest with NO glint.</li>
 *   <li>{@code 05_opacity} - nearly hidden: glint should be barely-there, not popped back to full.</li>
 * </ol>
 * Gated to the {@code fcgt} constant, currently wired for fabric-26.2.
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

            // The reported bug case at 40% opacity, captured as a before/after pair with identical
            // framing via the glint-swap toggle. BEFORE (swap off) reproduces the vanish: the vanilla
            // glint's EQUAL depth test fails against our no-depth-write base. AFTER (swap on) re-issues
            // the glint on a depth-matched pipeline so it draws with the faded armor.
            context.runOnClient(client -> ArmorHiderRenderTypes.setGlintSwapEnabled(false));
            captureGlintPair(context, 0.4, "40_before");

            long swapsBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.armorGlintSwapCount());
            context.runOnClient(client -> ArmorHiderRenderTypes.setGlintSwapEnabled(true));
            captureGlintPair(context, 0.4, "40_after");
            long swapsAfter = context.computeOnClient(client -> ArmorHiderRenderTypes.armorGlintSwapCount());

            // Machine check (per this repo's smoke convention): the swap must actually fire while a
            // faded enchanted piece is on screen. If the wrap silently misses its target on a version
            // bump the glint just reverts to vanishing with no crash to notice.
            if (swapsAfter <= swapsBefore) {
                throw new IllegalStateException(
                        "[smoke/fcgt] armor glint swap never fired for a faded enchanted piece (delta "
                                + (swapsAfter - swapsBefore) + ") - the armorEntityGlint wrap missed its"
                                + " target; the glint would silently vanish on translucent armor");
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] glint swaps during 40%-after capture: {}", swapsAfter - swapsBefore);

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
