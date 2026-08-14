package de.zannagh.armorhider.client.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.util.ExponentialBackoff;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

//? if >= 1.20.5 {
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
//? if < 1.20.5 {
/*import de.zannagh.armorhider.net.LegacyPacketHandler;
import de.zannagh.armorhider.net.CompressedJsonCodec;
import de.zannagh.armorhider.net.packets.CombatLogEventPacket;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
*///?}

/**
 * Sends packets from the client to the server without Fabric API, gated on a server handshake.
 * <p>
 * Armor Hider does not blindly emit custom payloads: sending an unknown channel to a vanilla (or
 * otherwise non-Armor-Hider) server can get the client disconnected. So every outgoing packet is held
 * until the server proves it runs the mod by sending a
 * {@link de.zannagh.armorhider.net.packets.HandshakePacket} (surfaced as
 * {@link ClientCommunicationManager#SERVER_SUPPORTS_MOD}). Packets sent before that decision are queued;
 * once the handshake arrives they are flushed in order, and once {@link #HANDSHAKE_TIMEOUT_MILLIS} elapse
 * with no handshake the server is treated as unsupported and every outgoing packet is dropped for the rest
 * of the connection. {@link #reset()} clears this state on every connect and disconnect, so a reconnect
 * always re-tests the new server.
 * <p>
 * All sends run under {@link #MONITOR}, so they are strictly ordered - a fast-path send can never overtake
 * the flush of packets that were queued before the handshake resolved.
 */
public final class ClientPacketSender {

    /** How long to wait for the server's handshake before deciding it does not run the mod. */
    private static final long HANDSHAKE_TIMEOUT_MILLIS = 10_000L;

    // Guards all of the mutable gate state below.
    private static final Object MONITOR = new Object();
    // Sends queued while the handshake decision is still pending.
    private static final Deque<Runnable> pending = new ArrayDeque<>();
    // null = undecided; TRUE = server supports the mod (send); FALSE = it does not (suppress).
    private static Boolean serverSupported = null;
    private static boolean waiterRunning = false;
    // Bumped on every reset() so a waiter left over from a previous connection can't commit a stale decision.
    private static long epoch = 0;

    private ClientPacketSender() {
    }

    /**
     * Forget the current connection's handshake result and re-gate from scratch. Called on both connect
     * and disconnect so reconnecting to a different server never reuses the previous decision.
     */
    public static void reset() {
        synchronized (MONITOR) {
            pending.clear();
            serverSupported = null;
            waiterRunning = false;
            epoch++;
        }
    }

    /**
     * Runs {@code sendAction} honoring the handshake gate: immediately if the server is known to support
     * the mod, never if it is known not to, or queued (starting the background waiter) while undecided.
     * Everything happens under {@link #MONITOR} to keep sends ordered.
     */
    private static void gate(Runnable sendAction) {
        synchronized (MONITOR) {
            if (Boolean.TRUE.equals(serverSupported)) {
                sendAction.run();
                return;
            }
            if (Boolean.FALSE.equals(serverSupported)) {
                ArmorHider.LOGGER.debug("Suppressing outgoing packet: server does not run Armor Hider.");
                return;
            }
            pending.add(sendAction);
            if (!waiterRunning) {
                waiterRunning = true;
                long myEpoch = epoch;
                Thread waiter = new Thread(() -> runWaiter(myEpoch), "armorhider-handshake-wait");
                waiter.setDaemon(true);
                waiter.start();
            }
        }
    }

    private static void runWaiter(long myEpoch) {
        boolean supported = awaitHandshake(myEpoch);
        synchronized (MONITOR) {
            if (myEpoch != epoch) {
                // reset() ran while we were waiting: a new connection owns the gate now, drop everything.
                return;
            }
            serverSupported = supported;
            waiterRunning = false;
            if (supported) {
                // Flush in submission order while holding the lock so no later fast-path send overtakes them.
                for (Runnable action : new ArrayList<>(pending)) {
                    action.run();
                }
            } else {
                ArmorHider.LOGGER.info(
                        "Server did not send an Armor Hider handshake within {} ms; suppressing outgoing packets.",
                        HANDSHAKE_TIMEOUT_MILLIS);
            }
            pending.clear();
        }
    }

    /** Polls {@link ClientCommunicationManager#SERVER_SUPPORTS_MOD} until it is set or the timeout elapses. */
    private static boolean awaitHandshake(long myEpoch) {
        if (ClientCommunicationManager.SERVER_SUPPORTS_MOD) {
            return true;
        }
        ExponentialBackoff backoff = new ExponentialBackoff((int) HANDSHAKE_TIMEOUT_MILLIS);
        // shouldContinue() sleeps between attempts (never under MONITOR), so this does not busy-spin.
        while (backoff.shouldContinue()) {
            synchronized (MONITOR) {
                if (myEpoch != epoch) {
                    return false;
                }
            }
            if (ClientCommunicationManager.SERVER_SUPPORTS_MOD) {
                return true;
            }
        }
        return ClientCommunicationManager.SERVER_SUPPORTS_MOD;
    }

    //? if >= 1.20.5 {
    public static void sendToServer(CustomPacketPayload payload) {
        gate(() -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null) {
                ArmorHider.LOGGER.debug("Cannot send packet {}: not connected to a server.", payload.type().id());
                return;
            }
            try {
                connection.send(new ServerboundCustomPayloadPacket(payload));
            } catch (UnsupportedOperationException e) {
                ArmorHider.LOGGER.debug("Server does not support packet {}, skipping.", payload.type().id());
            }
        });
    }
    //?}

    //? if < 1.20.5 {
    /*public static void sendToServer(PlayerConfig config) {
        gate(() -> sendLegacy(LegacyPacketHandler.getPlayerConfigChannel(), config));
    }

    public static void sendToServer(ServerWideSettings settings) {
        gate(() -> sendLegacy(LegacyPacketHandler.getServerWideSettingsChannel(), settings));
    }

    public static void sendToServer(CombatLogEventPacket combatLogPacket) {
        gate(() -> sendLegacy(LegacyPacketHandler.getCombatLogEventChannel(), combatLogPacket));
    }

    private static <T> void sendLegacy(Identifier channel, T payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            ArmorHider.LOGGER.debug("Cannot send packet on {}: not connected to a server.", channel);
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CompressedJsonCodec.encodeLegacy(payload, buf);
        connection.send(new ServerboundCustomPayloadPacket(channel, buf));
    }
    *///?}
}
