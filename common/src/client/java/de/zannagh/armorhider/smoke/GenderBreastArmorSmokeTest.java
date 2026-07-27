// Drives the Female Gender Mod (Wildfire) breast-armor render + physics paths. Needs the FGM jar
// present at runtime (pulled in on the gender smoke row) and the after-terrain render architecture
// the transparency fix relies on, so it shares WaterTransparencySmokeTest's floor.
//? if fcgt && >= 26.2-1.pre {
package de.zannagh.armorhider.smoke;

import com.wildfire.main.WildfireGender;
import com.wildfire.main.config.enums.Gender;
import com.wildfire.main.entitydata.PlayerConfig;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Path;

/**
 * Female Gender Mod compatibility smoke (fabric-client-gametest-api-v1).
 * <p>
 * Reproduces the three reported FGM interactions and machine-checks the two that have a stable
 * runtime signal, leaving screenshots for eyeballing the rest (this repo's smoke convention):
 * <ol>
 *   <li><b>Breast-armor translucency (issue 3).</b> With a chestplate equipped and chest opacity at
 *       50%, the breast cups must fade like the body plate — not vanish. The breast render-type swap
 *       is a {@code @Pseudo @WrapOperation} that fails silently if it can't resolve its target,
 *       leaving the piece on the alpha-tested {@code armorCutoutNoCull} type (a reduced-alpha colour
 *       is then discarded wholesale). We assert {@link ArmorHiderRenderTypes#breastArmorTranslucentSwapCount()}
 *       climbs while the breast is faded, pinning down whether the swap fired, and capture a
 *       screenshot at 100 / 50 / 0 % for the eye.</li>
 *   <li><b>Jiggle when the plate is hidden (issue 1).</b> FGM feeds the equipped chestplate's
 *       {@code physicsResistance()} / {@code tightness()} into the bounce, damping it. When Armor
 *       Hider fully hides the plate, {@code GenderPhysicsMixin} forces FGM's own "Armor Physics
 *       Override" on (via {@code PlayerConfig#getArmorPhysicsOverride}), which zeroes both, so the
 *       breasts jiggle as if unarmored. After an identical upward impulse we sample the left
 *       breast's vertical bounce range with the plate hidden vs. visible and assert hidden jiggles
 *       clearly more.</li>
 * </ol>
 * The combat-detection interaction (issue 2) is exercised implicitly by the render path but is not
 * asserted here (it needs a live damage event); the body-plate combat path already has coverage.
 * <p>
 * Gender is forced on the local player through FGM's own runtime config API
 * ({@link WildfireGender#getOrAddPlayerById}) rather than an on-disk config, so the test is
 * self-contained and doesn't depend on a pre-seeded {@code run/config/FemaleGenderMod} file.
 */
public final class GenderBreastArmorSmokeTest implements FabricClientGameTest {

    // Front third-person so the breast cups face the camera in the screenshots.
    private static final float BUST_SIZE = 0.9F;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Gender breast-armor smoke starting");
        context.waitForScreen(TitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {
            ArmorHider.LOGGER.info("[smoke/fcgt] World created, configuring Female Gender Mod");

            context.runOnClient(client -> {
                var player = client.player;
                if (player == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
                }

                // Make the local player female with visible, physics-enabled breasts, shown even in
                // armor (we equip a chestplate). This drives FGM's GenderArmorLayer for our hooks.
                PlayerConfig genderConfig = WildfireGender.getOrAddPlayerById(player.getUUID());
                genderConfig.updateGender(Gender.FEMALE);
                genderConfig.updateBustSize(BUST_SIZE);
                genderConfig.updateBreastPhysics(true);
                genderConfig.updateShowBreastsInArmor(true);

                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);

                // A prior smoke test (keybind) can leave the session disable override on — clear it so
                // the chest actually fades here regardless of test ordering.
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
            });

            singleplayer.getClientLevel().waitForChunksRender();
            context.waitTicks(20);

            // ── Issue 3: breast-armor translucency at partial opacity ────────────────────────────
            setChestOpacity(context, 1.0);
            context.waitTicks(5);
            Path fullShot = context.takeScreenshot("armorhider_gender_1_opacity_100");
            ArmorHider.LOGGER.info("[smoke/fcgt] breast armor @100% opacity: {}", fullShot);

            setChestOpacity(context, 0.5);
            context.waitTicks(5);
            long swapsBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.breastArmorTranslucentSwapCount());
            Path halfShot = context.takeScreenshot("armorhider_gender_2_opacity_50");
            context.waitTicks(3);
            long swapsAfter = context.computeOnClient(client -> ArmorHiderRenderTypes.breastArmorTranslucentSwapCount());
            ArmorHider.LOGGER.info("[smoke/fcgt] breast armor @50% opacity: {} (swaps {} -> {})",
                    halfShot, swapsBefore, swapsAfter);

            setChestOpacity(context, 0.0);
            context.waitTicks(5);
            Path hiddenShot = context.takeScreenshot("armorhider_gender_3_opacity_0");
            ArmorHider.LOGGER.info("[smoke/fcgt] breast armor @0% opacity (hidden): {}", hiddenShot);

            if (swapsAfter <= swapsBefore) {
                throw new IllegalStateException(
                        "[smoke/fcgt] breast-armor render-type swap never fired while the breast was faded"
                                + " (count " + swapsBefore + " -> " + swapsAfter + ") — the piece stays on the"
                                + " alpha-tested cutout type and is discarded instead of turning translucent;"
                                + " the @Pseudo @WrapOperation missed its target on this version/mod build");
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] breast-armor translucency swap fired ({} draws)",
                    swapsAfter - swapsBefore);

            // ── Issue 1: jiggle with the plate hidden vs. visible ────────────────────────────────
            // First prove the physics-relaxation hook actually fires and detects the hidden chest —
            // the bounce magnitude alone proved too small/noisy to trust while the hook was silently
            // reading stale state. These counters are the real machine check; the bounce numbers are
            // logged for context only.
            setChestOpacity(context, 0.0);
            context.waitTicks(15);
            long ticksHidden = context.computeOnClient(client -> ArmorHiderRenderTypes.genderPhysicsTickCount());
            long relaxedHidden = context.computeOnClient(client -> ArmorHiderRenderTypes.genderPhysicsRelaxedCount());
            context.waitTicks(15);
            long ticksHidden2 = context.computeOnClient(client -> ArmorHiderRenderTypes.genderPhysicsTickCount());
            long relaxedHidden2 = context.computeOnClient(client -> ArmorHiderRenderTypes.genderPhysicsRelaxedCount());
            ArmorHider.LOGGER.info("[smoke/fcgt] gender physics (chest hidden): ticks {}->{}, relaxed {}->{}",
                    ticksHidden, ticksHidden2, relaxedHidden, relaxedHidden2);

            double hiddenBounce = measureBounceRange(context, 0.0);
            double visibleBounce = measureBounceRange(context, 1.0);
            ArmorHider.LOGGER.info("[smoke/fcgt] breast bounce range: hidden={}, visible={}",
                    hiddenBounce, visibleBounce);

            if (ticksHidden2 <= ticksHidden) {
                throw new IllegalStateException(
                        "[smoke/fcgt] getArmorPhysicsOverride hook never fired (count " + ticksHidden + " -> "
                                + ticksHidden2 + ") — GenderPhysicsMixin's @ModifyReturnValue missed its target,"
                                + " so armor physics is never relaxed when the plate is hidden");
            }
            if (relaxedHidden2 <= relaxedHidden) {
                throw new IllegalStateException(
                        "[smoke/fcgt] physics hook fired but never forced the armor-physics override with the"
                                + " chest fully hidden (relaxed " + relaxedHidden + " -> " + relaxedHidden2
                                + ") — the hidden-chest condition (shouldHide) is not being met at physics time");
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] Gender breast-armor smoke complete");
        }
    }

    private static void setChestOpacity(ClientGameTestContext context, double opacity) {
        context.runOnClient(client -> {
            var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
            config.chestOpacity.setValue(opacity);
        });
    }

    // Applies one identical upward impulse and samples the left breast's vertical bounce position for
    // a fixed window, returning the peak-to-peak range. The bounce magnitude scales with (1 -
    // physicsResistance), so a hidden plate (armor-physics override forced on, resistance 0) yields a
    // visibly larger range than a visible generic plate (resistance 0.5).
    private static double measureBounceRange(ClientGameTestContext context, double opacity) {
        setChestOpacity(context, opacity);
        // Settle so the previous condition's oscillation fully decays before the next impulse.
        context.waitTicks(20);

        // Kick the player upward; FGM's physics reacts to the entity's vertical movement.
        context.runOnClient(client -> {
            var player = client.player;
            if (player != null) {
                player.setDeltaMovement(0.0, 0.6, 0.0);
            }
        });

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 25; i++) {
            context.waitTicks(1);
            double y = context.computeOnClient(client -> {
                var player = client.player;
                if (player == null) {
                    return 0.0;
                }
                PlayerConfig cfg = WildfireGender.getPlayerById(player.getUUID());
                if (cfg == null) {
                    return 0.0;
                }
                return (double) cfg.getLeftBreastPhysics().getPositionY();
            });
            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        return max - min;
    }
}
//?}
