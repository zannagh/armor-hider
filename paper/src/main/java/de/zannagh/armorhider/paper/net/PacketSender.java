package de.zannagh.armorhider.paper.net;

import com.google.gson.JsonElement;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Sends clientbound payloads.
 *
 * <p>{@code CraftPlayer.sendPluginMessage} silently drops anything the client has not announced in
 * its {@code minecraft:register}, and the mod never announces anything, so {@link ChannelSubscriber}
 * force-subscribes each connection to the union of both namespaces. Every send is still filtered
 * through {@link Player#getListeningPluginChannels()} - that is what makes this degrade to "send
 * nothing" rather than throw if the reflective force-subscribe ever fails.</p>
 *
 * <p>Because both aliases are subscribed, that filter alone would send every payload twice. That is
 * only worth avoiding for {@code ServerConfiguration}, so {@link #sendNarrowed} - and nothing else -
 * consults {@link ClientDialects} to pick the single alias matching the dialect the client was heard
 * speaking. Everything else goes out via {@link #send} on every subscribed alias, because a client
 * in the 1.21.4-1.21.10 range speaks different namespaces for different payload families and a
 * single recorded dialect does not describe it (see {@link Channels#DIALECT_BEARING_C2S}).</p>
 */
public final class PacketSender {

    private final Plugin plugin;
    private final ClientDialects dialects;

    public PacketSender(Plugin plugin, ClientDialects dialects) {
        this.plugin = plugin;
        this.dialects = dialects;
    }

    /**
     * Sends {@code payload} to {@code player} on every alias of {@code channels} the client listens
     * on, without narrowing to a dialect.
     *
     * <p>Use this for everything small. Narrowing exists purely to halve the wire cost of the one
     * genuinely large payload (see {@link #sendNarrowed}); for a permission packet or a combat-log
     * notification - a few bytes each - the duplicate alias costs nothing, and sending both is
     * correct no matter which namespace the client turns out to speak. Narrowing these was an
     * active hazard: a 1.21.4-1.21.10 client is recorded as speaking {@code armorhider} (from its
     * settings packet) but listens for permissions on {@code de.zannagh.armorhider} only, so the
     * narrowed send matched no listening channel and was dropped silently.</p>
     *
     * @return {@code true} if the payload reached at least one channel
     */
    public boolean send(Player player, List<String> channels, JsonElement payload) {
        return send(player, channels, payload, false);
    }

    /**
     * Sends {@code payload} on the aliases the client both listens on and - where known - speaks the
     * namespace of.
     *
     * <p>Reserved for {@code ServerConfiguration}, which reaches ~390 KiB gzipped on a large server
     * and is broadcast to everyone on every config change; sending it on both aliases would double
     * that. It is also the only payload family whose namespace actually tracks the client's era, so
     * it is the only one the recorded dialect describes correctly.</p>
     */
    public boolean sendNarrowed(Player player, List<String> channels, JsonElement payload) {
        return send(player, channels, payload, true);
    }

    private boolean send(Player player, List<String> channels, JsonElement payload, boolean narrow) {
        byte[] encoded;
        try {
            encoded = PayloadCodec.encode(payload);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to encode an armor-hider payload for "
                    + player.getUniqueId(), e);
            return false;
        }

        boolean sent = false;
        Collection<String> listening = player.getListeningPluginChannels();
        // Both aliases are deliberately subscribed (see ChannelSubscriber); narrowing to the dialect
        // the client was last heard speaking halves the wire cost of large broadcasts.
        List<String> targets = narrow ? dialects.select(player.getUniqueId(), channels) : channels;
        for (String channel : targets) {
            if (!listening.contains(channel)) {
                continue;
            }
            try {
                player.sendPluginMessage(plugin, channel, encoded);
                sent = true;
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send " + channel + " to "
                        + player.getUniqueId(), e);
            }
        }
        return sent;
    }

    /** Sends {@code payload} to every online player except {@code excluded}, on all aliases. */
    public void broadcastExcept(Collection<? extends Player> players, UUID excluded,
                                List<String> channels, JsonElement payload) {
        broadcastExcept(players, excluded, channels, payload, false);
    }

    /** As {@link #broadcastExcept}, narrowing to each recipient's dialect. {@code ServerConfiguration} only. */
    public void broadcastNarrowedExcept(Collection<? extends Player> players, UUID excluded,
                                        List<String> channels, JsonElement payload) {
        broadcastExcept(players, excluded, channels, payload, true);
    }

    private void broadcastExcept(Collection<? extends Player> players, UUID excluded,
                                 List<String> channels, JsonElement payload, boolean narrow) {
        for (Player player : players) {
            if (excluded != null && excluded.equals(player.getUniqueId())) {
                continue;
            }
            send(player, channels, payload, narrow);
        }
    }
}
