package de.zannagh.armorhider.paper.net;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Force-subscribes a connection to our channels, server-side.
 *
 * <p><strong>Why this exists.</strong> {@code CraftPlayer#sendPluginMessage} is guarded by
 * {@code if (this.channels().contains(channel))} - anything sent on a channel the client has not
 * announced via {@code minecraft:register} is dropped silently, with no exception and no log line.
 * The Armor Hider client deliberately bypasses Fabric API and writes raw custom payloads from
 * mixins, so it <em>never</em> sends {@code minecraft:register}: for every client in the wild,
 * {@code getListeningPluginChannels()} stays empty and {@code PlayerRegisterChannelEvent} never
 * fires for us. Without this class the plugin would look healthy and send nothing.</p>
 *
 * <p>{@code CraftPlayer#addChannel(String)} is public on every version - a direct method pre-26.x,
 * a {@code public default} inherited from {@code io.papermc.paper.connection.PluginMessageBridgeImpl}
 * on current main - so {@code getClass().getMethod(...)} finds both. Reflection also sidesteps the
 * 1.20.5 CraftBukkit package-relocation boundary, since no package name is ever named.</p>
 *
 * <p>The channel set lives on the <em>connection</em>, so this must run on every join.</p>
 */
public final class ChannelSubscriber {

    private static final String ADD_CHANNEL = "addChannel";

    private final Logger logger;
    private final List<String> channels;
    private volatile Method addChannel;
    private volatile boolean unavailableLogged;

    public ChannelSubscriber(Logger logger, List<String> channels) {
        this.logger = logger;
        this.channels = channels;
    }

    /**
     * Adds every Armor Hider channel to {@code player}'s connection so clientbound payloads are no
     * longer dropped. Failures are logged once and then swallowed - a broken reflection path must
     * not take the plugin or the join down with it.
     */
    public void subscribe(Player player) {
        Method method = resolve(player);
        if (method == null) {
            return;
        }
        for (String channel : channels) {
            try {
                method.invoke(player, channel);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                // Includes the 128-channel-per-connection cap (IllegalStateException). We add ~12,
                // so this only trips if something else already filled the connection's channel set.
                logOnce("Failed to force-subscribe " + player.getUniqueId() + " to " + channel
                        + " - clientbound Armor Hider payloads will be dropped for this player", e);
                return;
            }
        }
    }

    private Method resolve(Player player) {
        Method cached = addChannel;
        if (cached != null) {
            return cached;
        }
        try {
            Method resolved = player.getClass().getMethod(ADD_CHANNEL, String.class);
            resolved.setAccessible(true);
            addChannel = resolved;
            return resolved;
        } catch (NoSuchMethodException | RuntimeException | LinkageError e) {
            logOnce("Could not resolve CraftPlayer#" + ADD_CHANNEL + "(String) on this server. "
                    + "Clients that do not send minecraft:register will receive no Armor Hider "
                    + "payloads at all (server config, permissions, combat notifications)", e);
            return null;
        }
    }

    private void logOnce(String message, Throwable error) {
        if (unavailableLogged) {
            return;
        }
        unavailableLogged = true;
        logger.log(Level.WARNING, message, error);
    }
}
