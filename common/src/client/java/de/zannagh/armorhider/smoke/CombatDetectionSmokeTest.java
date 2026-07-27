//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.common.SlotModification;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Combat-detection smoke (fabric-client-gametest-api-v1).
 * <p>
 * Combat detection is meant to snap a player's hidden/faded armor back to fully visible when they
 * take or deal damage, then fade it back to the configured opacity over the combat window. The fade
 * side of that was implemented but never wired into rendering — {@code getCombatFade} existed with
 * no caller — so with the default config (combat detection on, "use default model" off) a damage
 * event produced no visible change at all. This test locks the whole chain down end to end:
 * <ol>
 *   <li>the client damage hook fires and registers a combat event for the player
 *       ({@code isInCombat} flips true),</li>
 *   <li>the registered event actually reaches opacity resolution — a chest configured to 0%
 *       (fully hidden) resolves to ~full opacity and stops reporting {@code shouldHide} while the
 *       player is in combat.</li>
 * </ol>
 * Both halves matter: asserting only {@code isInCombat} would still pass with the fade unwired,
 * which is precisely the state this test was written to catch.
 * <p>
 * The world is created in SURVIVAL — a creative player ignores the damage that drives the feature.
 */
public final class CombatDetectionSmokeTest implements FabricClientGameTest {

    // Fully hidden, so the in-combat result is unambiguous: hidden -> visible is a much stronger
    // signal than a partial opacity nudge, and it also exercises the shouldHide flip.
    private static final double HIDDEN_OPACITY = 0.0;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Combat detection smoke starting");
        context.waitForScreen(TitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    // Survival: a creative player is immune to the generic damage used below.
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                    state.setGenerateStructures(false);
                })
                .create()) {

            context.runOnClient(client -> {
                var player = client.player;
                if (player == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
                }
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

                // A prior smoke test (keybind) can leave the session disable override on.
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                config.chestOpacity.setValue(HIDDEN_OPACITY);
                // Defaults, restated so the test documents (and does not silently depend on) them.
                config.enableCombatDetection.setValue(true);
            });

            // Plain tick wait rather than getClientLevel().waitForChunksRender(): this test asserts on
            // config/state resolution rather than pixels, and getClientLevel() only exists in newer
            // FCGT builds — waitTicks keeps the test compiling on every version that enables fcgt.
            context.waitTicks(20);

            // ── Baseline: out of combat the chest is hidden ──────────────────────────────────────
            boolean hiddenBefore = context.computeOnClient(client -> resolveChest().shouldHide());
            boolean inCombatBefore = context.computeOnClient(client -> isInCombat());
            ArmorHider.LOGGER.info("[smoke/fcgt] before damage: inCombat={}, chestHidden={}",
                    inCombatBefore, hiddenBefore);
            if (!hiddenBefore) {
                throw new IllegalStateException(
                        "[smoke/fcgt] chest at 0% opacity is not hidden before combat — test precondition"
                                + " failed, so the in-combat assertion below would be meaningless");
            }
            context.takeScreenshot("armorhider_combat_1_before_hidden");

            // ── Damage the player, which must put them in combat ─────────────────────────────────
            singleplayer.getServer().runOnServer(mcServer -> {
                var level = mcServer.overworld();
                var serverPlayer = mcServer.getPlayerList().getPlayers().get(0);
                serverPlayer.hurtServer(level, level.damageSources().generic(), 4.0F);
            });
            context.waitTicks(5);

            boolean inCombatAfter = context.computeOnClient(client -> isInCombat());
            double transparencyAfter = context.computeOnClient(client -> resolveChest().transparency());
            boolean hiddenAfter = context.computeOnClient(client -> resolveChest().shouldHide());
            ArmorHider.LOGGER.info("[smoke/fcgt] after damage: inCombat={}, chestTransparency={}, chestHidden={}",
                    inCombatAfter, transparencyAfter, hiddenAfter);
            context.takeScreenshot("armorhider_combat_2_after_visible");

            if (!inCombatAfter) {
                throw new IllegalStateException(
                        "[smoke/fcgt] player is not in combat after taking damage — the client damage hook"
                                + " (LivingEntityMixin#triggerCombat on handleDamageEvent) never fired or never"
                                + " registered a combat event");
            }
            // The fade starts at fully opaque and decays back to the configured opacity, so straight
            // after the hit this must be far above the configured 0%.
            if (transparencyAfter < 0.9) {
                throw new IllegalStateException(
                        "[smoke/fcgt] in-combat chest transparency is " + transparencyAfter + ", expected ~1.0 —"
                                + " the combat event is registered but never reaches opacity resolution"
                                + " (combat fade not wired into SlotModification)");
            }
            if (hiddenAfter) {
                throw new IllegalStateException(
                        "[smoke/fcgt] chest still reports shouldHide while in combat — combat detection does not"
                                + " restore hidden armor, so a hidden piece stays invisible during combat");
            }

            ArmorHider.LOGGER.info("[smoke/fcgt] Combat detection checks passed");
        }
    }

    private static SlotModification resolveChest() {
        String name = ArmorHiderClient.getCurrentPlayerName();
        var client = net.minecraft.client.Minecraft.getInstance();
        ItemStack chest = client.player != null
                ? client.player.getItemBySlot(EquipmentSlot.CHEST)
                : ItemStack.EMPTY;
        return SlotModification.of(name, EquipmentSlot.CHEST, chest);
    }

    private static boolean isInCombat() {
        return ArmorHiderApi.getInstance().getCombatManagement()
                .isInCombat(ArmorHiderClient.getCurrentPlayerName());
    }
}
//?}
