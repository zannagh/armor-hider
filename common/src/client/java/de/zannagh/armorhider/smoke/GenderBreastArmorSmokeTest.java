// Drives the Female Gender Mod (Wildfire) breast-armor render + physics paths. Needs the FGM jar
// present at runtime (pulled in on the gender smoke row) and the after-terrain render architecture
// the transparency fix relies on, so it shares WaterTransparencySmokeTest's floor.
//? if fcgt && >= 26.2-1.pre {
package de.zannagh.armorhider.smoke;

import com.wildfire.main.WildfireGender;
import com.wildfire.main.config.enums.Gender;
import com.wildfire.main.entitydata.PlayerConfig;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.impl.AhRenderStateImpl;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.nio.file.Path;

/**
 * Female Gender Mod compatibility smoke (fabric-client-gametest-api-v1).
 * <p>
 * Reproduces the three reported FGM interactions and machine-checks the two that have a stable
 * runtime signal, leaving screenshots for eyeballing the rest (this repo's smoke convention):
 * <ol>
 *   <li><b>Breast-armor translucency (issue 3).</b> With a chestplate equipped and chest opacity at
 *       50%, the breast cups must fade like the body plate - not vanish. The breast render-type swap
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
 *   <li><b>Combat fade (issue 2).</b> After a damage event the cups must ride the same opacity ramp
 *       back down as the body plate, rather than staying opaque for the whole combat window and then
 *       snapping to hidden. Asserted mid-ramp via the same swap counter - see
 *       {@code assertFadesDuringCombat}.</li>
 *   <li><b>Armored-elytra combat.</b> The "Elytra Armor" datapack forges a chestplate+elytra into an
 *       {@code Items.ELYTRA} worn in the chest that also carries a chestplate asset, so FGM renders
 *       breast armor for it and {@code ItemInfo.isElytra()} is true - which routes the breast
 *       interception through the ELYTRA scope, not ARMOR_PIECE. With the swap/cleanup keyed off
 *       ARMOR_PIECE only, the faded breast was discarded (stuck invisible through combat) and the
 *       ELYTRA scope leaked. {@code assertArmoredElytraFollowsCombat} asserts the breast is drawn
 *       during combat and the scope does not leak.</li>
 * </ol>
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
        // Self-skip when FGM is absent (e.g. compat=none): this entrypoint is registered by MC version,
        // not by compat, so it also runs on bare rows - and touching WildfireGender there would throw
        // NoClassDefFoundError and red the whole batch. Guard BEFORE any FGM class is referenced.
        if (!CompatManager.requiresCompatTo(CompatFlags.GENDER_MOD)) {
            ArmorHider.LOGGER.info("[smoke/fcgt] Gender breast-armor smoke skipped: Female Gender Mod not present");
            return;
        }
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

                // A prior smoke test (keybind) can leave the session disable override on - clear it so
                // the chest actually fades here regardless of test ordering.
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
            });

            // fabric-client-gametest-api 6.0 (26.3-snapshot-8) dropped TestSingleplayerContext.getClientLevel();
            // the chunk-wait moved onto the TestServerConnection returned by getConnection().
            //? if >= 26.3-0.snapshot.8 {
            /*singleplayer.getConnection().waitForChunksRender();
            *///?} else {
            singleplayer.getClientLevel().waitForChunksRender();
            //?}
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
                                + " (count " + swapsBefore + " -> " + swapsAfter + ") - the piece stays on the"
                                + " alpha-tested cutout type and is discarded instead of turning translucent;"
                                + " the @Pseudo @WrapOperation missed its target on this version/mod build");
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] breast-armor translucency swap fired ({} draws)",
                    swapsAfter - swapsBefore);

            // ── Issue 1: jiggle with the plate hidden vs. visible ────────────────────────────────
            // First prove the physics-relaxation hook actually fires and detects the hidden chest -
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
                                + ticksHidden2 + ") - GenderPhysicsMixin's @ModifyReturnValue missed its target,"
                                + " so armor physics is never relaxed when the plate is hidden");
            }
            if (relaxedHidden2 <= relaxedHidden) {
                throw new IllegalStateException(
                        "[smoke/fcgt] physics hook fired but never forced the armor-physics override with the"
                                + " chest fully hidden (relaxed " + relaxedHidden + " -> " + relaxedHidden2
                                + ") - the hidden-chest condition (shouldHide) is not being met at physics time");
            }
            assertFadesDuringCombat(context, singleplayer);
            assertArmoredElytraFollowsCombat(context, singleplayer);

            ArmorHider.LOGGER.info("[smoke/fcgt] Gender breast-armor smoke complete");
        }
    }

    // ── Issue 2 (armored elytra): breast must follow combat on an elytra-armor chest ─────────────
    // The "Elytra Armor" datapack forges a chestplate+elytra into an "armored elytra": an Items.ELYTRA
    // worn in the chest that also carries a humanoid chestplate asset, so FGM renders breast armor for
    // it AND ItemInfo.isElytra() is true. Because isElytra() is true, ArmorHiderItemRenderer.intercept
    // delegates the breast interception to the ELYTRA renderer - so the scope entered for the breast is
    // ELYTRA, not ARMOR_PIECE. The bug: GenderArmorLayerMixin's render-type swap and RETURN cleanup keyed
    // off ARMOR_PIECE only, so (1) the swap never fired -> the faded breast stayed on the alpha-tested
    // cutout type and was discarded, i.e. the breast was INVISIBLE all through combat instead of snapping
    // back to visible, and (2) the entered ELYTRA scope leaked every frame. This reproduces the user
    // report ("armored elytra + gender mod: chest stuck invisible after entering combat"). Both halves
    // are asserted: the swap must climb (breast drawn) and the ELYTRA scope must not leak. EMF-agnostic -
    // the delegation is independent of EMF.
    private static void assertArmoredElytraFollowsCombat(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        context.runOnClient(client -> {
            var player = client.player;
            if (player == null) {
                throw new IllegalStateException("[smoke/fcgt] client player missing for armored-elytra combat test");
            }
            // Faithful "armored elytra": an elytra item that also equips as a chestplate with a humanoid
            // armor asset (copied off a diamond chestplate), so it renders both wings and breast armor.
            ItemStack armoredElytra = new ItemStack(Items.ELYTRA);
            var chestplateEquippable = new ItemStack(Items.DIAMOND_CHESTPLATE).get(DataComponents.EQUIPPABLE);
            if (chestplateEquippable == null) {
                throw new IllegalStateException("[smoke/fcgt] diamond chestplate has no EQUIPPABLE component -"
                        + " cannot build armored elytra for combat test");
            }
            armoredElytra.set(DataComponents.EQUIPPABLE, chestplateEquippable);
            player.setItemSlot(EquipmentSlot.CHEST, armoredElytra);

            var cfg = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(ArmorHiderClient.getCurrentPlayerName());
            cfg.enableCombatDetection.setValue(true);
            cfg.inCombatUseDefaultModel.setValue(false);
            // The armored elytra reads as "contains elytra", so the chest opacity only applies to it when
            // the elytra toggle is on - otherwise SlotModification bails out and there is nothing to hide.
            cfg.opacityAffectingElytra.setValue(true);
        });
        setChestOpacity(context, 0.0);
        // Wait out any combat left over from assertFadesDuringCombat so the baseline below is clean.
        for (int i = 0; i < 24 && context.computeOnClient(client -> isInCombat()); i++) {
            context.waitTicks(10);
        }
        context.waitTicks(10);

        // Baseline: hidden + out of combat, the breast must not be drawn (swap flat).
        long swapIdle0 = context.computeOnClient(client -> ArmorHiderRenderTypes.breastArmorTranslucentSwapCount());
        context.waitTicks(10);
        long swapIdle1 = context.computeOnClient(client -> ArmorHiderRenderTypes.breastArmorTranslucentSwapCount());
        if (swapIdle1 != swapIdle0) {
            throw new IllegalStateException("[smoke/fcgt] armored-elytra breast is being drawn while hidden and out"
                    + " of combat (swaps " + swapIdle0 + " -> " + swapIdle1 + ") - baseline precondition failed");
        }

        long leakBefore = context.computeOnClient(client -> AhRenderStateImpl.leakedScopeClears(RenderScope.ELYTRA));
        singleplayer.getServer().runOnServer(mcServer -> {
            var level = mcServer.overworld();
            var serverPlayer = mcServer.getPlayerList().getPlayers().get(0);
            serverPlayer.setGameMode(GameType.SURVIVAL);
            serverPlayer.hurtServer(level, level.damageSources().generic(), 4.0F);
        });
        context.waitTicks(5);

        double transparencyOnHit = context.computeOnClient(client -> resolveChestTransparency());
        if (!context.computeOnClient(client -> isInCombat()) || transparencyOnHit < 0.9) {
            throw new IllegalStateException("[smoke/fcgt] armored-elytra chest did not snap to ~full opacity after"
                    + " damage (inCombat=" + context.computeOnClient(client -> isInCombat()) + ", transparency="
                    + transparencyOnHit + ") - combat detection never reached opacity resolution");
        }

        // Ride the ramp until partly transparent, then assert the breast render-type swap fires - the
        // breast is actually being DRAWN (faded), not staying invisible - AND the ELYTRA scope did not leak.
        double transparencyMid = transparencyOnHit;
        for (int i = 0; i < 20 && transparencyMid > 0.9; i++) {
            context.waitTicks(10);
            transparencyMid = context.computeOnClient(client -> resolveChestTransparency());
        }
        long swapsBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.breastArmorTranslucentSwapCount());
        context.waitTicks(5);
        long swapsAfter = context.computeOnClient(client -> ArmorHiderRenderTypes.breastArmorTranslucentSwapCount());
        long leakAfter = context.computeOnClient(client -> AhRenderStateImpl.leakedScopeClears(RenderScope.ELYTRA));
        Path shot = context.takeScreenshot("armorhider_gender_6_armoredelytra_combat");
        ArmorHider.LOGGER.info("[smoke/fcgt] armored-elytra combat (transparency {}): swaps {} -> {}, ELYTRA leaks {} -> {} ({})",
                transparencyMid, swapsBefore, swapsAfter, leakBefore, leakAfter, shot);

        if (swapsAfter <= swapsBefore) {
            throw new IllegalStateException(
                    "[smoke/fcgt] armored-elytra breast was never drawn while in combat at transparency "
                            + transparencyMid + " (swaps " + swapsBefore + " -> " + swapsAfter + ") - the breast"
                            + " interception was routed to the ELYTRA scope but the render-type swap keys off"
                            + " ARMOR_PIECE, so the faded breast is discarded and stays invisible through combat");
        }
        if (leakAfter > leakBefore) {
            throw new IllegalStateException(
                    "[smoke/fcgt] ELYTRA render scope leaked during armored-elytra combat (" + leakBefore + " -> "
                            + leakAfter + ") - interceptBreastArmor entered the ELYTRA scope (isElytra delegation)"
                            + " but its RETURN handler only exited ARMOR_PIECE, bleeding alpha onto later submits");
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] armored-elytra breast follows combat, no scope leak ({} draws)",
                swapsAfter - swapsBefore);
    }

    private static boolean isInCombat() {
        return ArmorHiderApi.getInstance().getCombatManagement()
                .isInCombat(ArmorHiderClient.getCurrentPlayerName());
    }

    // ── Issue 2: the breast cups must follow the post-damage combat fade ─────────────────────────
    // Combat detection snaps every configured piece to full opacity on damage and then ramps it back
    // down over the combat window (CombatManager#transformTransparencyBasedOnCombat, resolved inside
    // SlotModification.of). The body plate always did this; the breast cups used to short-circuit on
    // shouldEnforceVanillaRendering() before interception, which meant they rendered fully opaque for
    // the entire window and then popped straight to hidden when the combat event expired. Asserting
    // the translucent swap fires *mid-ramp* is what distinguishes the ramp from that binary behaviour.
    // Runs last: it puts the player in combat for the fade window, which would unhide the chest the
    // physics section above depends on.
    private static void assertFadesDuringCombat(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        setChestOpacity(context, 0.5);
        context.runOnClient(client -> {
            // Neither is a default. enableCombatDetection is what CombatDetectionSmokeTest turns on the
            // same way; inCombatUseDefaultModel is what makes this a regression test rather than a
            // tautology - it is the setting that gates shouldEnforceVanillaRendering(), and the old
            // short-circuit only fired when a user had it enabled.
            var combatConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
            combatConfig.enableCombatDetection.setValue(true);
            combatConfig.inCombatUseDefaultModel.setValue(true);
        });
        context.waitTicks(5);

        singleplayer.getServer().runOnServer(mcServer -> {
            var level = mcServer.overworld();
            var serverPlayer = mcServer.getPlayerList().getPlayers().get(0);
            // The world is created in creative for the render/physics sections above, and a creative
            // player shrugs off generic damage - no damage event, no combat, nothing to assert.
            serverPlayer.setGameMode(GameType.SURVIVAL);
            serverPlayer.hurtServer(level, level.damageSources().generic(), 4.0F);
        });
        context.waitTicks(5);

        double transparencyOnHit = context.computeOnClient(client -> resolveChestTransparency());
        if (transparencyOnHit < 0.9) {
            throw new IllegalStateException(
                    "[smoke/fcgt] chest transparency is " + transparencyOnHit + " right after damage, expected ~1.0"
                            + " - combat detection never reached opacity resolution, so the fade assertion below"
                            + " would not be testing the fade");
        }

        // Ramp is linear over the combat window (10s by default), so a couple of seconds in the
        // resolved transparency sits between the configured 50% and full opacity.
        double transparencyMidFade = transparencyOnHit;
        for (int i = 0; i < 20 && transparencyMidFade > 0.95; i++) {
            context.waitTicks(10);
            transparencyMidFade = context.computeOnClient(client -> resolveChestTransparency());
        }
        if (transparencyMidFade > 0.95 || transparencyMidFade < 0.5) {
            throw new IllegalStateException(
                    "[smoke/fcgt] chest transparency never entered the mid-fade band (last value "
                            + transparencyMidFade + ") - cannot tell whether the breast armor follows the ramp");
        }

        long swapsBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.breastArmorTranslucentSwapCount());
        context.waitTicks(5);
        long swapsAfter = context.computeOnClient(client -> ArmorHiderRenderTypes.breastArmorTranslucentSwapCount());
        Path fadeShot = context.takeScreenshot("armorhider_gender_4_combat_fade");
        ArmorHider.LOGGER.info("[smoke/fcgt] breast armor mid combat fade (transparency {}): {} (swaps {} -> {})",
                transparencyMidFade, fadeShot, swapsBefore, swapsAfter);

        if (swapsAfter <= swapsBefore) {
            throw new IllegalStateException(
                    "[smoke/fcgt] breast-armor render-type swap never fired mid combat fade (count " + swapsBefore
                            + " -> " + swapsAfter + ") at transparency " + transparencyMidFade + " - the breast cups"
                            + " ignore the combat ramp and stay fully opaque until the combat event expires, then"
                            + " snap to hidden, while the body plate fades smoothly");
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] breast armor follows the combat fade ({} draws)",
                swapsAfter - swapsBefore);
    }

    private static double resolveChestTransparency() {
        Minecraft client = Minecraft.getInstance();
        ItemStack chest = client.player != null
                ? client.player.getItemBySlot(EquipmentSlot.CHEST)
                : ItemStack.EMPTY;
        return SlotModification.of(ArmorHiderClient.getCurrentPlayerName(), EquipmentSlot.CHEST, chest).transparency();
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
