// Drives the First Person Model (tr7zw) compat guards. Needs the FPM jar present at runtime, which is
// what the `firstperson` constant tracks - the same property that compiles FirstPersonCompat's typed
// branch, so test and guard are enabled together or not at all.
//? if fcgt && firstperson {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
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
 *   <li><b>Guards off.</b> With FPM 2.7.2 the same scene must still enter no HEAD scope for the camera
 *       body ({@link ArmorHiderRenderTypes#firstPersonHeadScopeEntryCount()} stays flat) and leak
 *       nothing: FPM blanks the worn head during its body render ({@code PlayerMixin.getItemBySlot}
 *       returns EMPTY for HEAD while {@code isRenderingPlayer}), so the extracted state carries no head
 *       item and there is no head scope to leak, guard or not. The worn head is asserted present via a
 *       direct {@code getItemBySlot(HEAD)} read outside FPM's rendering flag, so "no scope entered" is
 *       not vacuous. If this ever climbs, FPM has started rendering the worn head again and the leak
 *       hazard the guard exists for is back - re-validate the guard, do not weaken this check.</li>
 * </ol>
 * A worn player head plus a partial helmet opacity is the setup that would make the head scope non-empty
 * in third person: the HEAD scope keys off {@code IdentityCarrier#customHeadItem()}, not the armor slot.
 * <p>
 * Verified on a real GPU on 2026-09-16 (fabric-1.21.11, FPM 2.7.2 alone and with EMF/ETF/Iris): the
 * unguarded scene never reproduces a head-scope leak, for the reason above - the earlier "leak must
 * reappear with guards off" control was vacuous and has been replaced.
 */
public final class FirstPersonSmokeTest implements FabricClientGameTest {

    private static final double HEAD_OPACITY = 0.5;
    private static final int RENDER_TICKS = 40;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] First Person Model compat smoke starting");
        // Self-skip when FPM is absent at runtime (e.g. compat=none). The entrypoint registers wherever
        // firstperson.version is pinned, so it also runs on bare rows where FPM's guards can never fire -
        // that must read as "skipped", not the "guards never fired" failure (which means FPM is present
        // but dormant). Presence is the runtime class probe, so it distinguishes the two.
        if (!CompatManager.requiresCompatTo(CompatFlags.FIRST_PERSON_MODEL)) {
            ArmorHider.LOGGER.info("[smoke/fcgt] First Person Model compat smoke skipped: FPM not present");
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

            // ── Guards off: FPM's own head-slot blanking leaves no head scope to leak ────────────
            context.runOnClient(client -> {
                // Read outside FPM's rendering flag (runOnClient runs between frames, never inside
                // renderEntities), so this sees the real slot: the head must actually be worn, or the
                // "no head scope entered" check below would be vacuous.
                ItemStack worn = client.player.getItemBySlot(EquipmentSlot.HEAD);
                if (!worn.is(Items.PLAYER_HEAD)) {
                    throw new IllegalStateException("[smoke/fcgt] the player is not wearing the player head"
                            + " (HEAD slot is " + worn + ") - cannot judge the first-person head scope");
                }
                ArmorHiderRenderTypes.setFirstPersonGuardsEnabled(false);
            });
            context.waitTicks(10);
            long unguardedLeaksBefore = context.computeOnClient(client -> AhRenderStateImpl.leakedScopeClears(RenderScope.HEAD));
            long headEntriesBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.firstPersonHeadScopeEntryCount());
            context.waitTicks(RENDER_TICKS);
            long unguardedLeaksAfter = context.computeOnClient(client -> AhRenderStateImpl.leakedScopeClears(RenderScope.HEAD));
            long headEntriesAfter = context.computeOnClient(client -> ArmorHiderRenderTypes.firstPersonHeadScopeEntryCount());
            context.takeScreenshot("armorhider_firstperson_2_unguarded");
            context.runOnClient(client -> ArmorHiderRenderTypes.setFirstPersonGuardsEnabled(true));
            ArmorHider.LOGGER.info("[smoke/fcgt] guards off: camera-body HEAD scope entries {} -> {}, HEAD leaks {} -> {}",
                    headEntriesBefore, headEntriesAfter, unguardedLeaksBefore, unguardedLeaksAfter);

            // Scoped to the camera entity only (the counter is recorded solely for FPM's first-person body,
            // never for remote or third-person players, which legitimately enter head scopes).
            if (headEntriesAfter > headEntriesBefore) {
                throw new IllegalStateException(
                        "[smoke/fcgt] a HEAD scope was entered for FPM's camera body with the guards disabled ("
                                + headEntriesBefore + " -> " + headEntriesAfter + "). FPM 2.7.2 blanks the worn head"
                                + " during its body render (PlayerMixin.getItemBySlot returns EMPTY for HEAD while"
                                + " isRenderingPlayer), so no head scope should exist there to leak. FPM has started"
                                + " rendering the worn head again: the leak hazard FirstPersonCompat guards against is"
                                + " back - re-validate the guard, do not weaken this check");
            }
            if (unguardedLeaksAfter > unguardedLeaksBefore) {
                throw new IllegalStateException(
                        "[smoke/fcgt] HEAD scopes leaked with the guards disabled (" + unguardedLeaksBefore + " -> "
                                + unguardedLeaksAfter + ") although no camera-body head scope was entered - a"
                                + " different cancelled render path is entering the head scope");
            }

            ArmorHider.LOGGER.info("[smoke/fcgt] First Person Model compat smoke complete "
                    + "({} guard hits, {} camera-body head scopes, {} leaks)",
                    guardsAfter - guardsBefore, headEntriesAfter - headEntriesBefore,
                    unguardedLeaksAfter - unguardedLeaksBefore);
        }
    }
}
//?}
