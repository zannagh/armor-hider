package de.zannagh.armorhider.netPackets;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A generic packet codec that serializes objects to compressed JSON.
 * This avoids the 32767 character limit of PacketCodecs.STRING while maintaining
 * the generic approach of JSON serialization.
 */
public class CompressedJsonCodec {

    /**
     * Creates a PacketCodec that serializes objects to compressed JSON.
     *
     * @param gson The Gson instance to use for serialization
     * @param clazz The class type to deserialize to
     * @param <T> The type of object to serialize/deserialize
     * @return A PacketCodec for the given type
     */
    public static <T> PacketCodec<ByteBuf, T> create(Gson gson, Class<T> clazz) {
        return PacketCodec.of(
                (value, buf) -> encode(gson, value, buf),
                (buf) -> decode(gson, buf, clazz)
        );
    }

    private static <T> void encode(Gson gson, T value, ByteBuf buf) {
        try {
            // Serialize to JSON and compress
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
                 OutputStreamWriter writer = new OutputStreamWriter(gzipStream, StandardCharsets.UTF_8)) {
                gson.toJson(value, writer);
            }

            // Write compressed data to buffer
            byte[] compressed = byteStream.toByteArray();
            buf.writeInt(compressed.length);
            buf.writeBytes(compressed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode compressed JSON", e);
        }
    }

    private static <T> T decode(Gson gson, ByteBuf buf, Class<T> clazz) {
        try {
            // Read compressed data from buffer
            int length = buf.readInt();
            byte[] compressed = new byte[length];
            buf.readBytes(compressed);

            // Decompress and deserialize
            ByteArrayInputStream byteStream = new ByteArrayInputStream(compressed);
            try (GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
                 InputStreamReader reader = new InputStreamReader(gzipStream, StandardCharsets.UTF_8)) {
                return gson.fromJson(reader, clazz);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode compressed JSON", e);
        }
    }
}
