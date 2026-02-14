package de.zannagh.armorhider.netPackets;

import de.zannagh.armorhider.ArmorHider;
import io.netty.buffer.ByteBuf;
//? if >= 1.20.5 {
import net.minecraft.network.codec.StreamCodec;
//?}

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    private static <T> void encode(ByteBuf byteBuf, T value) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
                 OutputStreamWriter writer = new OutputStreamWriter(gzipStream, StandardCharsets.UTF_8)) {
                ArmorHider.GSON.toJson(value, writer);
            }

            byte[] compressed = byteStream.toByteArray();
            byteBuf.writeInt(compressed.length);
            byteBuf.writeBytes(compressed);
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
                return ArmorHider.GSON.fromJson(reader, clazz);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode compressed JSON", e);
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
