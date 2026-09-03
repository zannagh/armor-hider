package de.zannagh.armorhider.paper;

import de.zannagh.armorhider.paper.net.ArmorHiderPaperPackets;
import de.zannagh.armorhider.paper.net.ChannelSubscriber;
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
 * Drives the initial state push on join.
 *
 * <p>The primary path is {@link PlayerJoinEvent}: the client never sends {@code minecraft:register}
 * (see {@link ChannelSubscriber}), so the plugin force-subscribes the connection itself and then
 * pushes the {@code ServerConfiguration} snapshot and the recipient's permission level.
 * {@link PlayerRegisterChannelEvent} is kept as a secondary path for any future client that
 * <em>does</em> announce properly - and because {@code CraftPlayer#addChannel} fires that event
 * internally, both paths run for the same join. The per-player latches below make the push
 * idempotent, so nothing is ever sent twice.</p>
 *
 * <p>There is no handshake push here any more: eunomia's built-in {@code eunomia:hello} handshake
 * handler (installed by {@code enableServerHandshake()}) answers the client's probe automatically
 * once the transport is wired, so the client detects the server's capabilities on its own.</p>
 */
public final class PlayerConnectionListener implements Listener {

    private final ArmorHiderService service;
    private final ChannelSubscriber subscriber;
    private final Set<UUID> configurationSent = ConcurrentHashMap.newKeySet();
    private final Set<UUID> permissionsSent = ConcurrentHashMap.newKeySet();
    /**
     * Players currently inside {@link ChannelSubscriber#subscribe(Player)}.
     *
     * <p>{@code CraftPlayer#addChannel} fires {@link PlayerRegisterChannelEvent} synchronously, once
     * per channel, so without this guard the push would happen <em>from inside</em> the subscribe
     * loop - at which point only the channels added so far are listening, and the idempotence latch
     * would then suppress the resend. Deferring to the explicit push in {@link #onJoin} makes the
     * send happen once, with the full channel set already subscribed.</p>
     */
    private final Set<UUID> subscribing = ConcurrentHashMap.newKeySet();

    public PlayerConnectionListener(ArmorHiderService service, ChannelSubscriber subscriber) {
        this.service = service;
        this.subscriber = subscriber;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        // The channel set lives on the connection, so a reconnect starts from scratch.
        configurationSent.remove(id);
        permissionsSent.remove(id);

        subscribing.add(id);
        try {
            subscriber.subscribe(player);
        } finally {
            subscribing.remove(id);
        }
        // Shared render rules go first: the joiner's stale entry from a previous session is cleared on
        // the other clients here, independently of eunomia's capability handshake (which now gates the
        // joiner's own outgoing re-announcement client-side). Clearing on join means a rejoin can never
        // leave a stale snapshot standing on the other clients.
        service.syncSharedRulesOnJoin(player);
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
        if (ArmorHiderPaperPackets.SERVER_CONFIG.channelKey().equals(channel)) {
            pushConfiguration(event.getPlayer());
        }
        if (ArmorHiderPaperPackets.PERMISSION.channelKey().equals(channel)) {
            pushPermissions(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        configurationSent.remove(id);
        permissionsSent.remove(id);
        subscribing.remove(id);
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
