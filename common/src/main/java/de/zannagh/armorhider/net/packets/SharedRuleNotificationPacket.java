//? if >= 1.20.5 {
package de.zannagh.armorhider.net.packets;

import de.zannagh.armorhider.net.CompressedJsonCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Clientbound relay of one player's {@link SharedRuleStatePacket}.
 *
 * <p>{@link #playerName} and {@link #playerId} are filled in by the server from the authenticated
 * connection, never from the sender's payload. Receivers key the state by name, because that is how
 * every other render decision identifies a player; the id travels along so the entry can be replaced
 * and pruned reliably when a display name changes or is shared.</p>
 *
 * <p>An empty {@link #overrides} list clears the named player's entry.</p>
 *
 * @since 0.13.0
 */
public class SharedRuleNotificationPacket implements CustomPacketPayload {

    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "shared_rules_s2c_packet");
    public static final StreamCodec<ByteBuf, SharedRuleNotificationPacket> STREAM_CODEC = CompressedJsonCodec.create(SharedRuleNotificationPacket.class);

    public static final Type<SharedRuleNotificationPacket> TYPE = new Type<>(PACKET_IDENTIFIER);

    public String playerName;

    public UUID playerId;

    public List<SharedRuleOverride> overrides = new ArrayList<>();

    public long timestamp;

    /** Gson. */
    public SharedRuleNotificationPacket() {
    }

    public SharedRuleNotificationPacket(String playerName, UUID playerId, List<SharedRuleOverride> overrides, long timestamp) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.overrides = overrides != null ? new ArrayList<>(overrides) : new ArrayList<>();
        this.timestamp = timestamp;
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//?}

//? if < 1.20.5 {
/*package de.zannagh.armorhider.net.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SharedRuleNotificationPacket {

    public String playerName;

    public UUID playerId;

    public List<SharedRuleOverride> overrides = new ArrayList<>();

    public long timestamp;

    public SharedRuleNotificationPacket() {
    }

    public SharedRuleNotificationPacket(String playerName, UUID playerId, List<SharedRuleOverride> overrides, long timestamp) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.overrides = overrides != null ? new ArrayList<>(overrides) : new ArrayList<>();
        this.timestamp = timestamp;
    }
}
*///?}
