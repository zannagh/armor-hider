package de.zannagh.armorhider.paper.net;

import de.zannagh.eunomia.networking.packets.PacketType;
import de.zannagh.eunomia.networking.comms.ServerTransport;
import de.zannagh.eunomia.networking.serialization.PayloadCodec;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Server to client send path over Bukkit plugin messaging - the Paper implementation of eunomia's
 * {@link ServerTransport}.
 *
 * <p>Encoding runs through the shared {@link PayloadCodec}, so the bytes are byte-for-byte identical
 * to what the loader's native codec produces (bare {@code gzip(json)}, no length prefix) and the
 * modded client decodes them with no special-casing.</p>
 *
 * <p>{@code CraftPlayer#sendPluginMessage} silently drops anything the client has not announced in its
 * {@code minecraft:register}, and the armor-hider client never announces anything, so
 * {@link ChannelSubscriber} force-subscribes every connection first. Every send is still filtered
 * through {@link Player#getListeningPluginChannels()} - that is what makes a failed reflective
 * force-subscribe degrade to "send nothing" rather than throw.</p>
 */
public final class PaperServerTransport implements ServerTransport {

    private final Plugin plugin;

    public PaperServerTransport(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public <T> void sendToPlayer(UUID playerId, PacketType<T> type, T data) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            send(player, type, data);
        }
    }

    @Override
    public <T> void broadcast(PacketType<T> type, T data) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            send(player, type, data);
        }
    }

    @Override
    public <T> void broadcastExcept(UUID excludedPlayerId, PacketType<T> type, T data) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.getUniqueId().equals(excludedPlayerId)) {
                send(player, type, data);
            }
        }
    }

    /**
     * Encodes and sends a clientbound payload to one player, skipping the send if the connection is
     * not listening on the channel. Public so {@link ArmorHiderServerContext#reply} reuses it.
     */
    public void send(Player player, PacketType<?> type, Object data) {
        String channel = type.channelKey();
        if (!player.getListeningPluginChannels().contains(channel)) {
            return;
        }
        byte[] encoded;
        try {
            // Clientbound: held to the 1 MiB ceiling, not the tight 32 KiB serverbound one - the
            // ServerConfiguration snapshot reaches ~390 KiB on a large server.
            encoded = PayloadCodec.encode(data, false);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to encode an armor-hider payload on "
                    + channel + " for " + player.getUniqueId(), e);
            return;
        }
        try {
            player.sendPluginMessage(plugin, channel, encoded);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send " + channel + " to "
                    + player.getUniqueId(), e);
        }
    }
}
