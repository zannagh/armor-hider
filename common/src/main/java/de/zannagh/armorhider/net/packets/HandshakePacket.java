package de.zannagh.armorhider.net.packets;

import de.zannagh.armorhider.net.CompressedJsonCodec;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

//? if >= 1.20.5 {
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? }

/**
 * Server -&gt; client announcement that the server runs Armor Hider. The client treats the mere receipt
 * of this packet as proof that the server supports the mod and only then starts sending its own packets
 * (see {@code ClientCommunicationManager} / {@code ClientPacketSender}); the payload fields are purely
 * diagnostic. Sent on player join by both the Fabric/NeoForge server ({@code CommsManager}) and the
 * Paper plugin, so a client can never lock itself out of a server that actually supports the mod.
 */
public class HandshakePacket
        //? if >= 1.20.5
        implements CustomPacketPayload
{

    //? if >= 1.20.5 {
    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "handshake_s2c_packet");
    public static final StreamCodec<ByteBuf, HandshakePacket> STREAM_CODEC = CompressedJsonCodec.create(HandshakePacket.class);

    public static final Type<HandshakePacket> TYPE = new Type<>(PACKET_IDENTIFIER);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    //?}

    public UUID sessionId;
    public long timestamp;

    /**
     * Creates a fresh handshake. The {@code sessionId} is a per-handshake nonce (not a persistent id):
     * the client never needs it to be stable, only present, so it is generated here rather than derived
     * from server state - which also keeps construction identical across all supported game versions.
     */
    public HandshakePacket() {
        this.sessionId = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
    }
}
