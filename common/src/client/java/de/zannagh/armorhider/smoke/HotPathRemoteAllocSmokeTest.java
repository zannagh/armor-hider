//? if fcgt {
package de.zannagh.armorhider.smoke;

import com.mojang.authlib.GameProfile;
import de.zannagh.armorhider.AhAllocProbe;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/**
 * Regression test for the remote-player config-resolution allocation storm.
 * <p>
 * Sibling of {@code HotPathAllocSmokeTest}, which covers the LOCAL player only. The local player is the
 * cheap case: {@code AhPlayerConfigApiImpl.resolveConfig(name)} returns the shared {@code CURRENT} instance
 * without allocating. Every OTHER player falls through to {@code resolveUnknownPlayerConfig}, which - with
 * {@code usePlayerSettingsWhenUndeterminable} at its default of {@code true} - returned
 * {@code CURRENT.deepCopy(name, id)}: a whole fresh {@code PlayerConfig} graph plus a full
 * {@code ExclusionItemConfiguration.deepCopy()}, once per call, i.e. once per rendered player per frame.
 * Measured here before the fix: <b>5032 allocations for one remote player over 60 ticks</b>, exactly 1:1 with
 * the non-local resolve count, and linear in the player count (4 players cost 4x while the frame rate fell
 * ~41%). {@code ResolvedConfigCache} memoises those resolutions, so the cost is now per <i>player</i>, not
 * per <i>call</i>.
 * <p>
 * Two legs, both asserting:
 * <ul>
 *   <li><b>steady state</b> - one remote player already resolved during warmup; over the next 60 ticks the
 *       render path must allocate essentially nothing however many times it asks.</li>
 *   <li><b>cold multi-player</b> - three further remote players appear and the probe is armed immediately,
 *       with no warmup, so their first resolutions land inside the window. The cost must be on the order of
 *       the number of newly-seen players, not of the frame count. This is also the leg that originally proved
 *       the linear-in-player-count scaling.</li>
 * </ul>
 * Each leg asserts a positive control (the render path really did resolve non-local names) so a dead render
 * path cannot make it pass vacuously.
 * <p>
 * <b>Fidelity note.</b> The remote players are genuine client-side entities that the vanilla renderer draws
 * through the normal player-entity path, so the render-side call counts are real. What this is NOT is a real
 * multiplayer session: there is no server-side player list entry for them, so
 * {@code ArmorHiderClient.isPlayerRemotePlayer} answers {@code false} and resolution short-circuits into
 * {@code resolveUnknownPlayerConfig} one step earlier than it would on a live vanilla server. That is the
 * same terminal branch a real vanilla-server player reaches (no server-transmitted config exists there
 * either), so the allocation cost measured here is the cost that path pays.
 * <p>
 * Run it with {@code -Psmoke.fcgt.only=hot-path-remote-alloc}.
 */
public final class HotPathRemoteAllocSmokeTest implements FabricClientGameTest {

    private static final int MEASURE_TICKS = 60;

    private static final int WARMUP_TICKS = 20;

    private static final String REMOTE_NAME = "AhRemoteProbe";

    /** Remote players present for the warmed-up (steady-state) leg. */
    private static final int WARM_REMOTE_PLAYERS = 1;

    /** Further remote players introduced for the cold leg, measured from their very first frame. */
    private static final int COLD_REMOTE_PLAYERS = 3;

    /**
     * Allocation budget for the steady-state leg. The players' configs were built during warmup, so the
     * measured window should cost exactly 0; the single unit of slack absorbs one cache invalidation (a
     * server-config packet or a config save landing mid-window would legitimately rebuild the entries).
     * Measured with the fix in place: 0 allocations against 5224 non-local resolves.
     */
    private static final long WARM_ALLOCATION_BUDGET = WARM_REMOTE_PLAYERS + 1L;

    /**
     * Allocation budget for the cold leg: one build per newly-seen player, plus the same single unit of
     * invalidation slack, plus one for the warm player in case the same invalidation rebuilds it too.
     * The point of the assertion is the order of magnitude - single digits against thousands of resolves -
     * not a precise count, so it is deliberately expressed in players rather than as a round number.
     */
    private static final long COLD_ALLOCATION_BUDGET = COLD_REMOTE_PLAYERS + WARM_REMOTE_PLAYERS + 1L;

    /**
     * The measurement only means something if the render path asked for non-local configs far more often than
     * the allocation budget allows. Both legs measured >5000 non-local resolves per 60-tick window on the CI
     * software renderer; require a conservative fraction of that so the ratio, not just the count, is proven.
     */
    private static final long MIN_REMOTE_RESOLVES = 500;

    /** Synthetic entity id for the first probe player; ids count DOWN from here. */
    private static final int FIRST_REMOTE_ID = -90210;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Remote-player allocation regression test starting");
        context.waitForScreen(TitleScreen.class);

        try (var singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {
            context.runOnClient(HotPathRemoteAllocSmokeTest::prepareScene);

            // Leg 1: the remote player has been resolved for WARMUP_TICKS before the probe arms, so every
            // resolution inside the measured window must be a cache hit.
            context.waitTicks(WARMUP_TICKS);
            context.runOnClient(client -> {
                boolean present = client.level != null && client.level.getEntity(FIRST_REMOTE_ID) != null;
                ArmorHider.LOGGER.info("[smoke/fcgt] remote probe entity present after warmup: {}", present);
                AhAllocProbe.enable();
            });
            context.waitTicks(MEASURE_TICKS);
            context.runOnClient(client -> assertLeg("steady-state", WARM_ALLOCATION_BUDGET));

            // Leg 2: three more players appear and the probe arms in the same client tick, so their first
            // resolutions are inside the window. Cost must track the player count, not the frame count.
            context.runOnClient(client -> {
                var level = client.level;
                if (level == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client level vanished before the cold leg");
                }
                for (int i = 0; i < COLD_REMOTE_PLAYERS; i++) {
                    spawnRemote(level, WARM_REMOTE_PLAYERS + i, client);
                }
                AhAllocProbe.enable();
            });
            context.waitTicks(MEASURE_TICKS);
            context.runOnClient(client -> assertLeg("cold-multi-player", COLD_ALLOCATION_BUDGET));

            ArmorHider.LOGGER.info("[smoke/fcgt] Remote-player allocation regression test finished");
        } finally {
            AhAllocProbe.disable();
        }
    }

    /** Equips the viewer, puts the camera behind them, and spawns the warm-leg remote players. */
    private static void prepareScene(Minecraft client) {
        var player = client.player;
        var level = client.level;
        if (player == null || level == null) {
            throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
        }
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        for (int i = 0; i < WARM_REMOTE_PLAYERS; i++) {
            spawnRemote(level, i, client);
        }

        // Keep a real modification live for BOTH players: the local viewer's helmet opacity is what the
        // remote players' resolved copies inherit, so this also keeps the remote armour on the modified
        // render path rather than the vanilla short-circuit.
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
        config.helmetOpacity.setValue(0.5);
    }

    /** Adds one fully-armoured client-side remote player next to the viewer. */
    private static void spawnRemote(ClientLevel level, int index, Minecraft client) {
        var player = client.player;
        if (player == null) {
            throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
        }
        var profile = new GameProfile(UUID.randomUUID(), REMOTE_NAME + index);
        var remote = new RemotePlayer(level, profile);
        remote.setId(FIRST_REMOTE_ID - index);
        remote.setPos(player.getX() + 1.5 + index, player.getY(), player.getZ() + 1.5);
        remote.setYRot(player.getYRot());
        remote.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        remote.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        remote.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        remote.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        level.addEntity(remote);
    }

    /**
     * Reads and disarms the probe, then asserts the positive control and the allocation budget for one leg.
     *
     * @param leg    the leg's name, for the log line and the failure message.
     * @param budget the highest allocation count this leg may produce.
     */
    private static void assertLeg(String leg, long budget) {
        long allocations = AhAllocProbe.playerConfigAllocationCount();
        long resolves = AhAllocProbe.resolveConfigCallCount();
        long remoteResolves = AhAllocProbe.resolveConfigRemoteCallCount();
        AhAllocProbe.disable();

        ArmorHider.LOGGER.info(
                "[smoke/fcgt] REMOTE-ALLOC RESULT leg={} ticks={} resolveConfig_total={}"
                        + " resolveConfig_nonLocal={} PlayerConfig_allocations={} budget={}",
                leg, MEASURE_TICKS, resolves, remoteResolves, allocations, budget);

        if (remoteResolves < MIN_REMOTE_RESOLVES) {
            throw new IllegalStateException(
                    "[smoke/fcgt] leg '" + leg + "': the render path resolved only " + remoteResolves
                            + " non-local player names during the measured window (resolveConfig total="
                            + resolves + "), below the " + MIN_REMOTE_RESOLVES + " needed for the allocation"
                            + " assertion to mean anything - the spawned remote players were not rendered,"
                            + " so the measurement would be vacuous");
        }

        if (allocations > budget) {
            throw new IllegalStateException(
                    "[smoke/fcgt] leg '" + leg + "': the render path built " + allocations
                            + " PlayerConfig graphs over " + MEASURE_TICKS + " ticks (budget " + budget
                            + ") against " + remoteResolves + " non-local resolutions. Remote-player config"
                            + " resolution is no longer memoised - see ResolvedConfigCache - so every frame"
                            + " deep-copies a config per rendered player again.");
        }
    }
}
//?}
