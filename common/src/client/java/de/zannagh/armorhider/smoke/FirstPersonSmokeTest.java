// Drives the First Person Model (tr7zw) compat guards. Needs the FPM jar present at runtime, which is
// what the `firstperson` constant tracks - the same property that compiles FirstPersonCompat's typed
// branch, so test and guard are enabled together or not at all.
//? if fcgt && firstperson {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.impl.AhRenderStateImpl;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
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
 * First Person Model compatibility smoke (fabric-client-gametest-api-v1).
 * <p>
 * FPM renders the local player's body in first person by extracting an ordinary render state for the
 * camera entity, so our scopes and identity capture run exactly as in third person. The hazard is that
 * FPM then cancels several layer submits at their {@code HEAD} for that entity - {@code CustomHeadLayer}
 * unconditionally. Our scope-enter hooks sit at the same {@code HEAD} and are ordered ahead of FPM's, so
 * without a guard we enter a scope, FPM cancels the submit, and our {@code @At("RETURN")} release never
 * runs: the scope stays active until the next entity-render boundary sweeps it up, bleeding the worn
 * head's opacity onto whatever the rest of that entity render submits.
 * <p>
 * The experiment runs both ways in one pass, using {@code ArmorHiderRenderTypes}' test-only switch:
 * <ol>
 *   <li><b>Guards on.</b> {@link ArmorHiderRenderTypes#firstPersonLayerGuardCount()} must climb (proving
 *       the compat recognises FPM's first-person body and is actually declining scopes, not lying
 *       dormant) while {@link AhRenderStateImpl#leakedScopeClears} for {@link RenderScope#HEAD} stays
 *       flat.</li>
 *   <li><b>Guards off.</b> The same scene must now leak HEAD scopes - which is what proves the leak is
 *       real and the guard is what prevents it, rather than the scene never entering a head scope at
 *       all.</li>
 * </ol>
 * A worn player head plus a partial helmet opacity is the setup that makes the head scope non-empty:
 * the HEAD scope keys off {@code IdentityCarrier#customHeadItem()}, not the armor slot.
 */
public final class FirstPersonSmokeTest implements FabricClientGameTest {

    private static final double HEAD_OPACITY = 0.5;
    private static final int RENDER_TICKS = 40;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] First Person Model compat smoke starting");
        context.waitForScreen(TitleScreen.class);

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
                // A worn head (not a helmet) is what the HEAD scope resolves through.
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
                client.options.setCameraType(CameraType.FIRST_PERSON);

                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
                ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName())
                        .helmetOpacity.setValue(HEAD_OPACITY);
            });

            // waitTicks rather than getClientLevel().waitForChunksRender(): this test asserts on
            // counters rather than pixels, and getClientLevel() only exists in newer FCGT builds - the
            // plain wait keeps it compiling on every variant that pins firstperson.
            context.waitTicks(30);

            // ── Guards on: the compat fires, and nothing leaks ───────────────────────────────────
            long guardsBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.firstPersonLayerGuardCount());
            long leaksBefore = context.computeOnClient(client -> AhRenderStateImpl.leakedScopeClears(RenderScope.HEAD));
            context.waitTicks(RENDER_TICKS);
            long guardsAfter = context.computeOnClient(client -> ArmorHiderRenderTypes.firstPersonLayerGuardCount());
            long leaksAfter = context.computeOnClient(client -> AhRenderStateImpl.leakedScopeClears(RenderScope.HEAD));
            context.takeScreenshot("armorhider_firstperson_1_guarded");
            ArmorHider.LOGGER.info("[smoke/fcgt] guards on: guard hits {} -> {}, HEAD leaks {} -> {}",
                    guardsBefore, guardsAfter, leaksBefore, leaksAfter);

            if (guardsAfter <= guardsBefore) {
                throw new IllegalStateException(
                        "[smoke/fcgt] the First Person Model guards never fired (count " + guardsBefore + " -> "
                                + guardsAfter + ") - either FPM is not rendering the first-person body in this"
                                + " scene, or FirstPersonCompat no longer recognises its camera-entity flag"
                                + " (LivingEntityRenderStateAccess#isCameraEntity), leaving the compat dormant");
            }
            if (leaksAfter > leaksBefore) {
                throw new IllegalStateException(
                        "[smoke/fcgt] HEAD scopes leaked with the guards on (" + leaksBefore + " -> " + leaksAfter
                                + ") - a scope is still being entered for a layer submit that FPM cancels, so the"
                                + " worn head's opacity bleeds into the rest of the entity render");
            }

            // ── Guards off: the leak must reappear, or the assertion above proves nothing ────────
            context.runOnClient(client -> ArmorHiderRenderTypes.setFirstPersonGuardsEnabled(false));
            context.waitTicks(10);
            long unguardedLeaksBefore = context.computeOnClient(client -> AhRenderStateImpl.leakedScopeClears(RenderScope.HEAD));
            context.waitTicks(RENDER_TICKS);
            long unguardedLeaksAfter = context.computeOnClient(client -> AhRenderStateImpl.leakedScopeClears(RenderScope.HEAD));
            context.takeScreenshot("armorhider_firstperson_2_unguarded");
            context.runOnClient(client -> ArmorHiderRenderTypes.setFirstPersonGuardsEnabled(true));
            ArmorHider.LOGGER.info("[smoke/fcgt] guards off: HEAD leaks {} -> {}",
                    unguardedLeaksBefore, unguardedLeaksAfter);

            if (unguardedLeaksAfter <= unguardedLeaksBefore) {
                throw new IllegalStateException(
                        "[smoke/fcgt] no HEAD scope leaked with the guards disabled (" + unguardedLeaksBefore
                                + " -> " + unguardedLeaksAfter + ") - this scene does not reproduce the leak the"
                                + " guards exist for, so the guarded assertion above is vacuous. Either FPM stopped"
                                + " cancelling CustomHeadLayer#submit, or the head scope is no longer entered here");
            }

            ArmorHider.LOGGER.info("[smoke/fcgt] First Person Model compat smoke complete "
                    + "({} guard hits, {} leaks prevented)",
                    guardsAfter - guardsBefore, unguardedLeaksAfter - unguardedLeaksBefore);
        }
    }
}
//?}
