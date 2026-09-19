//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.AhAllocProbe;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.impl.AhRenderStateImpl;
import de.zannagh.armorhider.client.common.RenderScope;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Render hot-path allocation regression smoke.
 * <p>
 * Guards the fix for the frame-drop regression: {@code AhRenderStateImpl.getActiveScope(scope)} answered a
 * scope MISS - the overwhelmingly common case - with a freshly built {@code RenderScopeContext.empty(scope)},
 * which built a {@code SlotModification.empty()} and a {@code RenderModifications.empty()} (itself building a
 * second {@code SlotModification.empty()}), each running {@code new PlayerConfig()}: a ~37-allocation
 * constructor that also drew a {@code UUID.randomUUID()} from a synchronized {@code SecureRandom}. The hand
 * mixins ran that twice at the HEAD of every {@code ModelPart.render} (per model part, recursively, per
 * entity, per frame) and twice per baked quad of every item model.
 * <p>
 * <b>Why an allocation count and not a frame time.</b> CI renders under Mesa llvmpipe software GL on a
 * shared self-hosted runner, so absolute FPS/frame-time numbers are noise and any threshold on them is
 * either vacuous or flaky. The number of {@code PlayerConfig} graphs built during a render window, however,
 * is deterministic: the correct value is exactly zero, on any GPU, at any frame rate.
 * <p>
 * <b>Shape of the test.</b> Build a world, equip a full armour set and set a non-default helmet opacity so a
 * real modification is live (see below), let the pipeline warm up, then arm {@link AhAllocProbe} on the
 * client/render thread, render for {@link #MEASURE_TICKS} ticks, disarm, and assert the counter is 0.
 * <p>
 * <b>Why the live modification matters.</b> The fix has two halves and this test must exercise the expensive
 * one. With no scope carrying a modification the new {@code hasAnyScopeModification()} guard short-circuits
 * the mixins before any lookup happens, and the test would pass for the trivial reason that
 * {@code getActiveScope} is never called at all. Holding {@code helmetOpacity} at 50% keeps the ARMOR_PIECE
 * scope entering with a real modification, so the guard lets the mixins through and they do query OFFHAND
 * and HEAD - both of which miss, and both of which are the allocations that used to happen. The positive
 * control below asserts that ARMOR_PIECE really did move during the measured window, so a dead render
 * pipeline reports itself instead of silently satisfying a zero-allocation assertion.
 * <p>
 * <b>Why exactly zero, with no slack.</b> Everything reached on this path is now memoized
 * ({@code RenderScopeContext.empty}, {@code SlotModification.empty}) and the local player's config resolves
 * to the single {@code CURRENT} instance, so nothing on the client thread has a legitimate reason to build a
 * {@code PlayerConfig} during a steady-state render window. The probe additionally only counts on the thread
 * that armed it, so background config I/O and netty threads cannot colour the result. A non-zero reading is
 * therefore a real finding, not tolerable noise - do not paper over it with a constant.
 * <p>
 * Stonecutter-gated to {@code fcgt} only (no version floor): the two mixins that carried the regression,
 * {@code hand/ModelPartMixin} and {@code hand/ItemRendererMixin}, are themselves {@code //? if < 1.21.9},
 * so this must run on the oldest FCGT-capable variant that still has them - {@code fabric-1.21.4}.
 * Run it in isolation with {@code -Psmoke.fcgt.only=hot-path-alloc}.
 */
public final class HotPathAllocSmokeTest implements FabricClientGameTest {

    /** Ticks of steady-state rendering to measure over. ~3 seconds; long enough for many frames. */
    private static final int MEASURE_TICKS = 60;

    /** Ticks to let the pipeline settle (first-frame pipeline/texture setup) before arming the probe. */
    private static final int WARMUP_TICKS = 20;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Hot-path allocation smoke starting");
        context.waitForScreen(TitleScreen.class);

        try (var singleplayer = context.worldBuilder()
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
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                // A held item so ItemRendererMixin's per-baked-quad hooks run as well.
                player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));

                // Third person so the body/armour layer mixins fire (first person skips most layers).
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                // Keeps ARMOR_PIECE entering with a real modification for the whole window - see class doc.
                config.helmetOpacity.setValue(0.5);
            });

            context.waitTicks(WARMUP_TICKS);

            // Armed from the client thread: the probe watches exactly the thread that calls enable().
            long[] armorEntriesAtStart = new long[1];
            context.runOnClient(client -> {
                armorEntriesAtStart[0] = AhRenderStateImpl.modifiedScopeEnterCount(RenderScope.ARMOR_PIECE);
                AhAllocProbe.enable();
            });

            // Nothing but rendering happens in here - no config writes, no screenshots, no world I/O.
            context.waitTicks(MEASURE_TICKS);

            context.runOnClient(client -> {
                long allocations = AhAllocProbe.playerConfigAllocationCount();
                long armorEntries = AhRenderStateImpl.modifiedScopeEnterCount(RenderScope.ARMOR_PIECE)
                        - armorEntriesAtStart[0];
                AhAllocProbe.disable();

                ArmorHider.LOGGER.info(
                        "[smoke/fcgt] Hot-path window: {} ticks, ARMOR_PIECE modified entries {},"
                                + " PlayerConfig allocations on the render thread {}",
                        MEASURE_TICKS, armorEntries, allocations);

                // Positive control: without this, a dead render pipeline would pass the assertion below
                // for the wrong reason (nothing rendered, so nothing allocated).
                if (armorEntries == 0) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] ARMOR_PIECE scope was never entered with a modification during the"
                                    + " measured window - the render interception pipeline is dead on this"
                                    + " version, so the allocation assertion below would be vacuous");
                }

                if (allocations != 0) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] render hot path allocated " + allocations + " PlayerConfig graph(s)"
                                    + " over " + MEASURE_TICKS + " ticks (expected 0). Something on the render"
                                    + " path builds a config graph per model part / per baked quad again -"
                                    + " check that RenderScopeContext.empty() and SlotModification.empty()"
                                    + " still return memoized instances and that the hand mixins still guard"
                                    + " on AhRenderManagementApi.hasAnyScopeModification()");
                }
            });

            ArmorHider.LOGGER.info("[smoke/fcgt] Hot-path allocation smoke passed");
        } finally {
            // Never leave the probe armed for a sibling test in the same client launch.
            AhAllocProbe.disable();
        }
    }
}
//?}
