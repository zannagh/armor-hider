package de.zannagh.armorhider.net;

import de.zannagh.armorhider.ArmorHider;
import io.netty.buffer.ByteBuf;
//? if >= 1.20.5 {
import net.minecraft.network.codec.StreamCodec;
//?}

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressedJsonCodec {
    
    //? if >= 1.20.5 {
    // Creates a PacketCodec that serializes objects to compressed JSON.
    public static <T> StreamCodec<ByteBuf, T> create(Class<T> clazz) {
        return StreamCodec.of(
                CompressedJsonCodec::encode,
                (buf) -> decode(buf, clazz)
        );
    }
    //?}

    /**
     * Vanilla's clientbound custom-payload ceiling ({@code ClientboundCustomPayloadPacket.MAX_PAYLOAD_SIZE},
     * 1 MiB). Used as the upper sanity bound for any payload we encode or decode.
     */
    public static final int MAX_PAYLOAD_BYTES = 1048576;

    /**
     * Vanilla's <em>serverbound</em> ceiling ({@code ServerboundCustomPayloadPacket.MAX_PAYLOAD_SIZE}) is far
     * tighter at 32767, and a vanilla server — Realms included — decodes unknown payloads via
     * {@code DiscardedPayload}, which throws and disconnects the client for anything larger. Only C2S types
     * are held to this limit; the S2C {@code ServerConfiguration} broadcast legitimately runs much larger.
     * A little headroom is kept for the length prefix and framing.
     */
    public static final int MAX_SERVERBOUND_PAYLOAD_BYTES = 32767 - 256;

    /**
     * Ceiling on the <em>inflated</em> size of a decoded payload, guarding against a decompression bomb on
     * what is an attacker-controlled stream. Sized against real data: a 1500-player {@code ServerConfiguration}
     * measures ~14.8 MiB uncompressed (~38x compression), so 64 MiB leaves a comfortable 4x margin for
     * legitimately huge servers while still bounding memory use during parsing.
     */
    public static final int MAX_DECOMPRESSED_BYTES = 64 * 1024 * 1024;

    private static <T> void encode(ByteBuf byteBuf, T value) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
                 OutputStreamWriter writer = new OutputStreamWriter(gzipStream, StandardCharsets.UTF_8)) {
                ArmorHider.GSON.toJson(value, writer);
            }

            byte[] compressed = byteStream.toByteArray();
            // PlayerConfig is our only sizeable C2S payload, so it is the one held to the serverbound limit.
            // Refusing here beats letting a vanilla server kick the client on join. Backstop only: the
            // payload should no longer be able to get this big now that forNetwork() drops the exclusion map.
            int limit = value instanceof de.zannagh.armorhider.net.packets.PlayerConfig
                    ? MAX_SERVERBOUND_PAYLOAD_BYTES
                    : MAX_PAYLOAD_BYTES;
            if (compressed.length > limit) {
                throw new IllegalStateException("Refusing to encode an oversized armor-hider payload: "
                        + compressed.length + " bytes exceeds the " + limit + " byte limit");
            }
            byteBuf.writeInt(compressed.length);
            byteBuf.writeBytes(compressed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode compressed JSON", e);
        }
    }

    private static <T> T decode(ByteBuf buf, Class<T> clazz) {
        try {
            int length = buf.readInt();
            // The length prefix is attacker-controlled; allocating on it unchecked is a trivial OOM.
            if (length < 0 || length > MAX_PAYLOAD_BYTES || length > buf.readableBytes()) {
                throw new IllegalArgumentException("Rejecting an armor-hider payload with an implausible "
                        + "length of " + length + " bytes (" + buf.readableBytes() + " readable)");
            }
            byte[] compressed = new byte[length];
            buf.readBytes(compressed);

            ByteArrayInputStream byteStream = new ByteArrayInputStream(compressed);
            // Bounding the compressed length is not enough on its own: gzip routinely hits ~38x on this data
            // (measured) and a crafted stream reaches ~1000x, so a 1 MiB payload could otherwise inflate to
            // hundreds of megabytes during parsing. Cap the inflated side too.
            try (GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
                 InputStream boundedStream = new SizeLimitedInputStream(gzipStream, MAX_DECOMPRESSED_BYTES);
                 InputStreamReader reader = new InputStreamReader(boundedStream, StandardCharsets.UTF_8)) {
                T decoded = ArmorHider.GSON.fromJson(reader, clazz);
                // Configs arriving off the wire get the same repair pass as configs read from disk —
                // PlayerConfig.deserialize is bypassed entirely on this path.
                if (decoded instanceof de.zannagh.armorhider.net.packets.PlayerConfig playerConfig) {
                    de.zannagh.armorhider.net.packets.PlayerConfig.heal(playerConfig);
                }
                return decoded;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode compressed JSON", e);
        }
    }

    /**
     * Fails fast once more than {@code limit} bytes have been read, so an over-large stream is rejected
     * during inflation rather than after it has already been materialised in memory.
     */
    private static final class SizeLimitedInputStream extends FilterInputStream {

        private final long limit;
        private long consumed;

        private SizeLimitedInputStream(InputStream delegate, long limit) {
            super(delegate);
            this.limit = limit;
        }

        private void recordRead(long readCount) throws IOException {
            if (readCount <= 0) {
                return;
            }
            consumed += readCount;
            if (consumed > limit) {
                throw new IOException("Rejecting an armor-hider payload that inflates beyond " + limit
                        + " bytes — refusing to decompress further");
            }
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                recordRead(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            int readCount = super.read(buffer, off, len);
            recordRead(readCount);
            return readCount;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = super.skip(n);
            recordRead(skipped);
            return skipped;
        }
    }

    // Public encode method for legacy (1.20.x) packet handling.
    public static <T> void encodeLegacy(T value, ByteBuf buf) {
        encode(buf, value);
    }

    // Public decode method for legacy (1.20.x) packet handling.
    public static <T> T decodeLegacy(ByteBuf buf, Class<T> clazz) {
        return decode(buf, clazz);
    }
}
