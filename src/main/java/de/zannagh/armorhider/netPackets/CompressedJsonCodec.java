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

public class CompressedJsonCodec {

    private static volatile Gson GSON = new Gson();
    
    public static void setGson(Gson gson) {
        GSON = gson;
    }
    
    /**
     * Creates a PacketCodec that serializes objects to compressed JSON.
     *
     * @param clazz The class type to deserialize to
     * @param <T> The type of object to serialize/deserialize
     * @return A PacketCodec for the given type
     */
    public static <T> PacketCodec<ByteBuf, T> create(Class<T> clazz) {
        return PacketCodec.of(
                CompressedJsonCodec::encode,
                (buf) -> decode(buf, clazz)
        );
    }

    private static <T> void encode(T value, ByteBuf buf) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
                 OutputStreamWriter writer = new OutputStreamWriter(gzipStream, StandardCharsets.UTF_8)) {
                GSON.toJson(value, writer);
            }

            byte[] compressed = byteStream.toByteArray();
            buf.writeInt(compressed.length);
            buf.writeBytes(compressed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode compressed JSON", e);
        }
    }

    private static <T> T decode(ByteBuf buf, Class<T> clazz) {
        try {
            int length = buf.readInt();
            byte[] compressed = new byte[length];
            buf.readBytes(compressed);

            ByteArrayInputStream byteStream = new ByteArrayInputStream(compressed);
            try (GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
                 InputStreamReader reader = new InputStreamReader(gzipStream, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, clazz);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode compressed JSON", e);
        }
    }
}
