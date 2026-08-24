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

/**
 * Serverbound: what the sender's <em>shared</em> render rules currently resolve to for their own
 * equipment. Sent only when the outcome changes (and once per join, so a rejoin cannot leave a stale
 * snapshot standing on the other clients), and relayed on as a {@link SharedRuleNotificationPacket}.
 *
 * <p>An empty {@link #overrides} list is meaningful: it says "nothing of mine is rule-hidden any
 * more" and clears the sender's entry everywhere.</p>
 *
 * <p>{@link #playerName} is a diagnostic only. The server discards it and relays its own
 * authoritative name and the authenticated sender UUID, so a client cannot hide somebody else's
 * armor by claiming their name.</p>
 *
 * @since 0.13.0
 */
public class SharedRuleStatePacket implements CustomPacketPayload {

    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "shared_rules_c2s_packet");
    public static final StreamCodec<ByteBuf, SharedRuleStatePacket> STREAM_CODEC = CompressedJsonCodec.create(SharedRuleStatePacket.class);

    public static final Type<SharedRuleStatePacket> TYPE = new Type<>(PACKET_IDENTIFIER);

    public String playerName;

    public List<SharedRuleOverride> overrides = new ArrayList<>();

    public long timestamp;

    /** Gson. */
    public SharedRuleStatePacket() {
    }

    public SharedRuleStatePacket(String playerName, List<SharedRuleOverride> overrides) {
        this.playerName = playerName;
        this.overrides = overrides != null ? new ArrayList<>(overrides) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
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

public class SharedRuleStatePacket {

    public String playerName;

    public List<SharedRuleOverride> overrides = new ArrayList<>();

    public long timestamp;

    public SharedRuleStatePacket() {
    }

    public SharedRuleStatePacket(String playerName, List<SharedRuleOverride> overrides) {
        this.playerName = playerName;
        this.overrides = overrides != null ? new ArrayList<>(overrides) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
}
*///?}
