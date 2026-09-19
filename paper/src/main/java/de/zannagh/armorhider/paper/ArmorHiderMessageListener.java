package de.zannagh.armorhider.paper;

import de.zannagh.armorhider.paper.net.ArmorHiderServerContext;
import de.zannagh.armorhider.paper.net.PaperServerTransport;
import de.zannagh.eunomia.networking.comms.CommunicationManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Feeds inbound plugin messages into the eunomia {@link CommunicationManager}.
 *
 * <p>Bukkit hands every registered incoming channel to the same listener, so the channel name is the
 * {@code namespace:path} routing key. The manager decodes the raw {@code gzip(json)} bytes through the
 * shared {@link de.zannagh.eunomia.networking.serialization.PayloadCodec} and dispatches to the handler
 * registered for that channel - including eunomia's own {@code eunomia:hello}, which the handshake
 * handler answers automatically.</p>
 *
 * <p>A malformed or oversized payload is logged and dropped; it must never propagate out of here,
 * because an exception would kill the sender's connection.</p>
 */
public final class ArmorHiderMessageListener implements PluginMessageListener {

    private final Logger logger;
    private final PaperServerTransport transport;

    public ArmorHiderMessageListener(Logger logger, PaperServerTransport transport) {
        this.logger = logger;
        this.transport = transport;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        try {
            ArmorHiderServerContext context = new ArmorHiderServerContext(player, transport);
            CommunicationManager.dispatchServerboundRaw(channel, message, context);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Dropping a malformed armor-hider payload on " + channel
                    + " from " + player.getUniqueId() + ": " + e.getMessage());
        }
    }
}
