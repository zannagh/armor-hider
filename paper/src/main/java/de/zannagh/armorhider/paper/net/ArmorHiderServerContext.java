package de.zannagh.armorhider.paper.net;

import de.zannagh.eunomia.networking.packets.PacketType;
import de.zannagh.eunomia.networking.packets.ServerContext;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * The server-side context handed to a serverbound handler on Paper.
 *
 * <p>Exposes the Bukkit {@link Player} for handlers that need it (permission-level resolution, the
 * name-collision index); {@link #reply} sends straight back to the sender over plugin messaging. The
 * {@link #senderId() sender id} is the <em>authenticated</em> connection UUID, never a client-supplied
 * field, so a handler that keys its store on it cannot be spoofed.</p>
 */
public final class ArmorHiderServerContext implements ServerContext {

    private final Player player;
    private final PaperServerTransport transport;

    public ArmorHiderServerContext(Player player, PaperServerTransport transport) {
        this.player = player;
        this.transport = transport;
    }

    /** The sending Bukkit player. */
    public Player player() {
        return player;
    }

    @Override
    public UUID senderId() {
        return player.getUniqueId();
    }

    @Override
    public String senderName() {
        return player.getName();
    }

    @Override
    public <T> void reply(PacketType<T> type, T data) {
        transport.send(player, type, data);
    }
}
