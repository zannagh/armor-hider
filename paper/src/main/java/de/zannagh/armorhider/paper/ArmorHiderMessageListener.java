package de.zannagh.armorhider.paper;

import com.google.gson.JsonObject;
import de.zannagh.armorhider.paper.net.Channels;
import de.zannagh.armorhider.paper.net.ClientDialects;
import de.zannagh.armorhider.paper.net.PayloadCodec;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dispatches inbound plugin messages to {@link ArmorHiderService}.
 *
 * <p>Bukkit hands every registered incoming channel to the same listener, so the channel name - in
 * either namespace - selects the handler. A malformed or oversized payload is logged and dropped;
 * it must never propagate out of here, because that would kill the sender's connection.</p>
 *
 * <p>The channel's namespace is also the only signal of which dialect the client speaks, so it is
 * recorded in {@link ClientDialects} before anything else - even a payload we fail to decode still
 * proves which alias its sender uses.</p>
 */
public final class ArmorHiderMessageListener implements PluginMessageListener {

    private final Logger logger;
    private final ArmorHiderService service;
    private final ClientDialects dialects;

    public ArmorHiderMessageListener(Logger logger, ArmorHiderService service,
                                     ClientDialects dialects) {
        this.logger = logger;
        this.service = service;
        this.dialects = dialects;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        dialects.remember(player.getUniqueId(), channel);

        JsonObject payload;
        try {
            payload = PayloadCodec.decodeServerbound(message);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Dropping a malformed armor-hider payload on " + channel
                    + " from " + player.getUniqueId() + ": " + e.getMessage());
            return;
        }

        try {
            dispatch(channel, player, payload);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to handle an armor-hider payload on " + channel
                    + " from " + player.getUniqueId(), e);
        }
    }

    private void dispatch(String channel, Player player, JsonObject payload) {
        if (Channels.PLAYER_CONFIG_C2S.contains(channel)) {
            service.handlePlayerConfig(player, payload);
            return;
        }
        if (Channels.SERVER_WIDE_SETTINGS_C2S.contains(channel)) {
            service.handleServerWideSettings(player, payload);
            return;
        }
        if (Channels.COMBAT_LOG_C2S.contains(channel)) {
            service.handleCombatLogEvent(player, payload);
        }
    }
}
