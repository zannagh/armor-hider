package de.zannagh.armorhider.net;

/**
 * Wire size ceilings for armor-hider payloads. The actual enforcement now lives in eunomia's
 * {@code de.zannagh.eunomia.networking.serialization.PayloadCodec}; these mirror its constants so
 * tests (and any local sanity checks) can reason about the same limits without depending on the
 * eunomia-core internals directly.
 */
public final class NetworkLimits {

    private NetworkLimits() {
    }

    /**
     * Vanilla's clientbound custom-payload ceiling ({@code ClientboundCustomPayloadPacket.MAX_PAYLOAD_SIZE},
     * 1 MiB) - the upper sanity bound for any payload we encode or decode.
     */
    public static final int MAX_PAYLOAD_BYTES = 1048576;

    /**
     * Vanilla's <em>serverbound</em> ceiling ({@code ServerboundCustomPayloadPacket.MAX_PAYLOAD_SIZE}) is
     * far tighter at 32767, and a vanilla server - Realms included - decodes unknown payloads via
     * {@code DiscardedPayload}, which throws and disconnects the client for anything larger. Only C2S
     * types are held to this limit; the S2C {@code ServerConfiguration} broadcast legitimately runs much
     * larger. A little headroom is kept for the length prefix and framing.
     */
    public static final int MAX_SERVERBOUND_PAYLOAD_BYTES = 32767 - 256;

    /**
     * Ceiling on the <em>inflated</em> size of a decoded payload, guarding against a decompression bomb.
     */
    public static final int MAX_DECOMPRESSED_BYTES = 64 * 1024 * 1024;
}
