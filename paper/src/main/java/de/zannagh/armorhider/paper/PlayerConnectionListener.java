package de.zannagh.armorhider.paper;

import de.zannagh.armorhider.paper.net.ChannelSubscriber;
import de.zannagh.armorhider.paper.net.Channels;
import de.zannagh.armorhider.paper.net.ClientDialects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the initial state push.
 *
 * <p>The primary path is {@link PlayerJoinEvent}: the client never sends
 * {@code minecraft:register} (see {@link ChannelSubscriber}), so the plugin force-subscribes the
 * connection itself and then pushes. {@link PlayerRegisterChannelEvent} is kept as a secondary path
 * for any future client that <em>does</em> announce properly - and because
 * {@code CraftPlayer#addChannel} fires that event internally, both paths run for the same join.
 * The per-player latches below make the push idempotent, so nothing is ever sent twice.</p>
 */
public final class PlayerConnectionListener implements Listener {

    private final ArmorHiderService service;
    private final ChannelSubscriber subscriber;
    private final ClientDialects dialects;
    private final Set<UUID> configurationSent = ConcurrentHashMap.newKeySet();
    private final Set<UUID> permissionsSent = ConcurrentHashMap.newKeySet();
    /**
     * Players currently inside {@link ChannelSubscriber#subscribe(Player)}.
     *
     * <p>{@code CraftPlayer#addChannel} fires {@link PlayerRegisterChannelEvent} synchronously, once
     * per channel, so without this guard the push happens <em>from inside</em> the subscribe loop —
     * at which point only the aliases added so far are listening. The legacy alias is registered
     * first, so the snapshot went out on {@code armorhider:*} only, the idempotence latch then
     * suppressed the resend, and every client speaking the {@code de.zannagh.armorhider:*} dialect
     * (1.21.11+) received nothing at all — silently, since {@code sendPluginMessage} just skips
     * unlistened channels. Deferring to the explicit push in {@link #onJoin} makes the send happen
     * once, with the full alias set already subscribed.</p>
     */
    private final Set<UUID> subscribing = ConcurrentHashMap.newKeySet();

    public PlayerConnectionListener(ArmorHiderService service, ChannelSubscriber subscriber,
                                    ClientDialects dialects) {
        this.service = service;
        this.subscriber = subscriber;
        this.dialects = dialects;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        // The channel set lives on the connection, so a reconnect starts from scratch. The dialect
        // goes with it: the same player may reconnect from a different client version.
        configurationSent.remove(id);
        permissionsSent.remove(id);
        dialects.forget(id);

        subscribing.add(id);
        try {
            subscriber.subscribe(player);
        } finally {
            subscribing.remove(id);
        }
        // Send the handshake first so the client lifts its outgoing-traffic suppression as early as
        // possible. Unlike config/permissions it is not latched: it is cheap, the client is idempotent,
        // and it must be re-sent on every (re)connection.
        service.sendHandshake(player);
        pushConfiguration(player);
        pushPermissions(player);
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (subscribing.contains(event.getPlayer().getUniqueId())) {
            // Our own force-subscribe re-entering this handler mid-loop; onJoin pushes afterwards.
            return;
        }
        String channel = event.getChannel();
        if (Channels.SERVER_CONFIGURATION_S2C.contains(channel)) {
            pushConfiguration(event.getPlayer());
        }
        if (Channels.PERMISSIONS_S2C.contains(channel)) {
            pushPermissions(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        configurationSent.remove(id);
        permissionsSent.remove(id);
        subscribing.remove(id);
        dialects.forget(id);
    }

    private void pushConfiguration(Player player) {
        if (configurationSent.add(player.getUniqueId())) {
            service.sendServerConfiguration(player);
        }
    }

    private void pushPermissions(Player player) {
        if (permissionsSent.add(player.getUniqueId())) {
            service.sendPermissions(player);
        }
    }
}
