package de.zannagh.armorhider.client.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.impl.AhRenderRuleRegistryImpl;
import de.zannagh.armorhider.client.api.impl.AhRuleTarget;
import de.zannagh.armorhider.client.api.impl.AhSharedRuleStore;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.net.AhPackets;
import de.zannagh.armorhider.net.packets.SharedRuleOverride;
import de.zannagh.armorhider.net.packets.SharedRuleStatePacket;
import de.zannagh.eunomia.networking.comms.CommunicationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The sending half of shared render rules: evaluates the local player's
 * {@code AhRenderRuleBuilder.shared()} rules against themselves once per client tick and pushes the
 * result to the server whenever it changes, so the other clients can render this player the way the
 * rule says.
 *
 * <p>Predicates are arbitrary code and cannot be serialised, so an outcome is what travels. That is
 * also why this lives on the tick rather than on the render path: the value has to exist for the
 * local player whether or not anybody is currently drawing them.</p>
 *
 * <h2>Traffic</h2>
 * Sends are diffed against the last snapshot and rate limited, so a predicate that is simply
 * <em>true</em> every tick produces exactly one packet. Nothing is sent at all until some mod
 * registers a shared rule ({@link AhRenderRuleRegistryImpl#hasSharedRules()} is one volatile read),
 * and eunomia's send gate drops everything anyway on a server that does not run Armor Hider.
 */
public final class SharedRuleBroadcaster {

    /**
     * Floor between two sends. A predicate that flips every tick would otherwise be a 20 Hz packet
     * source; coalescing to 4 Hz keeps a hide visually immediate while bounding the traffic. The
     * pending state is not dropped, only delayed - {@link #lastSent} is the last state actually
     * <em>sent</em>, so a change suppressed by the floor is re-detected on the next tick.
     */
    private static final long MIN_SEND_INTERVAL_MILLIS = 250L;

    /** How often the received-state store is checked for players who have left. Two seconds. */
    private static final int PRUNE_INTERVAL_TICKS = 40;

    /**
     * The last snapshot handed to the server, or {@code null} when nothing has been
     * sent on this connection yet. The {@code null} state is what makes a rejoin re-announce: a
     * previous session's state may still be standing on the other clients.
     */
    private static @Nullable List<SharedRuleOverride> lastSent = null;

    private static long lastSendMillis = 0L;

    private static int tickCounter = 0;

    private SharedRuleBroadcaster() {
    }

    /** Forgets what was sent, so the next tick re-announces. Called on connect and disconnect. */
    public static void reset() {
        lastSent = null;
        lastSendMillis = 0L;
    }

    /**
     * One client tick's worth of work: prune what has gone stale, then re-announce the local player's
     * shared state if it changed.
     */
    public static void tick(@Nullable Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        pruneDepartedPlayers(minecraft);

        List<SharedRuleOverride> current;
        if (AhRenderRuleRegistryImpl.hasSharedRules()) {
            Player self = minecraft.player;
            if (self == null) {
                return;
            }
            current = evaluateLocalState(self);
        } else if (lastSent != null && !lastSent.isEmpty()) {
            // The last shared rule was unregistered while something was still announced. Retracting it
            // is not optional: the other clients hold whatever was last relayed, so returning here
            // because "there is nothing to share" would leave the player hidden on every other screen
            // for the rest of the session. Exactly one empty state goes out - the next tick sees an
            // empty lastSent and falls into the branch below.
            current = List.of();
        } else {
            // Nothing shared and nothing outstanding. This is the common case for every client whose
            // mods never asked for sharing, and it costs one volatile read.
            return;
        }

        if (current.equals(lastSent)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (lastSendMillis != 0L && now - lastSendMillis < MIN_SEND_INTERVAL_MILLIS) {
            // Rate limited, not dropped: `current` is not stored, so the next tick sees the same
            // difference again and sends it as soon as the floor has passed. The first send of a
            // connection is exempt so joining is not delayed by a quarter second for no reason.
            return;
        }

        // Stamped before the attempt, so a send that keeps failing retries on the same floor rather
        // than once per tick with a warning each time.
        lastSendMillis = now;
        lastSent = current;
        try {
            CommunicationManager.sendToServer(AhPackets.SHARED_RULES,
                    new SharedRuleStatePacket(ArmorHiderClient.getCurrentPlayerName(), current));
        } catch (Exception e) {
            // Never take the client tick down for this. Forgetting what was "sent" makes the next tick
            // retry, which is the right behaviour for a transient encoder or connection failure.
            lastSent = null;
            ArmorHider.LOGGER.warn("Could not send shared render rule state to the server.", e);
        }
    }

    /**
     * Evaluates every shared rule against the local player, one entry per target that resolves to
     * something. Targets that resolve to nothing are omitted rather than sent as no-ops, so the
     * snapshot equality check above is exact.
     */
    private static List<SharedRuleOverride> evaluateLocalState(Player self) {
        String playerName = ArmorHiderClient.getCurrentPlayerName();
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig();
        List<SharedRuleOverride> overrides = new ArrayList<>();

        for (AhRuleTarget target : AhRuleTarget.values()) {
            var slot = target.slot();
            var stack = self.getItemBySlot(slot);
            // ELYTRA is evaluated even when the chest slot holds a chestplate. Gating on "is this
            // actually an elytra" here would only move the check: a receiver consults the elytra target
            // solely while the player really is wearing plain wings, so an entry announced without them
            // is inert. Predicates that care see the real stack and can decide for themselves.
            boolean isElytra = target == AhRuleTarget.ELYTRA;
            double base = SlotModification.preRuleOpacityFor(config, playerName, slot, isElytra);
            var override = AhRenderRuleRegistryImpl.evaluateShared(
                    target, playerName, slot, stack, isElytra, config, base);
            if (override != null) {
                overrides.add(override);
            }
        }
        return overrides;
    }

    /**
     * Drops received state for players who have left. The mod has no server-side disconnect hook, and
     * the tab list makes this free on the client - see {@code AhSharedRuleStore#retainOnline}.
     */
    private static void pruneDepartedPlayers(Minecraft minecraft) {
        if (AhSharedRuleStore.isEmpty()) {
            tickCounter = 0;
            return;
        }
        if (++tickCounter < PRUNE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        var connection = minecraft.getConnection();
        if (connection == null) {
            return;
        }
        Set<UUID> online = new HashSet<>();
        for (var info : connection.getOnlinePlayers()) {
            //? if >= 1.21.9
            online.add(info.getProfile().id());
            //? if < 1.21.9
            //online.add(info.getProfile().getId());
        }
        if (online.isEmpty()) {
            // An empty player list is not evidence that everyone left - it is what a connection that has
            // not finished populating the tab list looks like. Pruning against it would drop every live
            // entry and only restore them when their owners next changed state.
            return;
        }
        if (AhSharedRuleStore.retainOnline(online)) {
            // A departed player's cached modification may still be baked into a PlayerModificationInfo
            // somewhere; a null name invalidates every player's cache, which is what the session toggle
            // does for the same reason.
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.notifyConfigListeners(null);
        }
    }
}
