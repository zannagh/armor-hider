package de.zannagh.armorhider.paper.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The mod's wire format, reimplemented without Minecraft classes.
 *
 * <pre>[int32 big-endian compressedLength][GZIP(UTF-8 JSON)]</pre>
 *
 * <p>Identical on every game version, and byte-for-byte compatible with the mod's
 * {@code CompressedJsonCodec}. All size guards from that class are reproduced here - this decodes
 * attacker-controlled input.</p>
 */
public final class PayloadCodec {

    /** Vanilla clientbound custom-payload ceiling; also Bukkit's {@code Messenger.MAX_MESSAGE_SIZE}. */
    public static final int MAX_PAYLOAD_BYTES = 1048576;

    /** Vanilla serverbound ceiling, with headroom kept for framing (tighter than Fabric / Neo). */
    public static final int MAX_SERVERBOUND_PAYLOAD_BYTES = 32767 - 256;

    /** Ceiling on the inflated size of a decoded payload - decompression-bomb guard. */
    public static final int MAX_DECOMPRESSED_BYTES = 64 * 1024 * 1024;

    /**
     * Matches {@code ArmorHider.GSON}'s {@code setPrettyPrinting()}. The plugin only ever handles
     * raw {@link JsonObject}s, so none of the mod's type-adapter factories are needed: config items
     * serialize as bare values and pass through untouched.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PayloadCodec() {
    }

    /** The pretty-printing Gson instance used for both the wire and the on-disk config. */
    public static Gson gson() {
        return GSON;
    }

    /**
     * Frames and compresses a clientbound payload.
     *
     * @throws IllegalStateException if the compressed payload exceeds {@link #MAX_PAYLOAD_BYTES}
     */
    public static byte[] encode(JsonElement value) {
        byte[] compressed = compress(value);
        if (compressed.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalStateException("Refusing to encode an oversized armor-hider payload: "
                    + compressed.length + " bytes exceeds the " + MAX_PAYLOAD_BYTES + " byte limit");
        }
        ByteArrayOutputStream framed = new ByteArrayOutputStream(compressed.length + 4);
        try (DataOutputStream out = new DataOutputStream(framed)) {
            out.writeInt(compressed.length);
            out.write(compressed);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode compressed JSON", e);
        }
        return framed.toByteArray();
    }

    /**
     * Decodes a serverbound payload, holding it to the serverbound size cap.
     *
     * @return the decoded object, never {@code null}
     * @throws IllegalArgumentException if the frame is malformed or exceeds any size guard
     */
    public static JsonObject decodeServerbound(byte[] raw) {
        return decode(raw, MAX_SERVERBOUND_PAYLOAD_BYTES);
    }

    private static JsonObject decode(byte[] raw, int maxCompressed) {
        if (raw == null || raw.length < 4) {
            throw new IllegalArgumentException("Rejecting a truncated armor-hider payload");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int length = in.readInt();
            // The length prefix is attacker-controlled; allocating on it unchecked is a trivial OOM.
            if (length < 0 || length > maxCompressed || length > raw.length - 4) {
                throw new IllegalArgumentException("Rejecting an armor-hider payload with an implausible "
                        + "length of " + length + " bytes (" + (raw.length - 4) + " readable)");
            }
            byte[] compressed = new byte[length];
            in.readFully(compressed);
            return inflate(compressed);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode compressed JSON", e);
        }
    }

    private static JsonObject inflate(byte[] compressed) throws IOException {
        try (GZIPInputStream gzipStream = new GZIPInputStream(new ByteArrayInputStream(compressed));
             InputStream bounded = new SizeLimitedInputStream(gzipStream, MAX_DECOMPRESSED_BYTES);
             Reader reader = new InputStreamReader(bounded, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonObject()) {
                throw new IllegalArgumentException("Rejecting an armor-hider payload that is not a JSON object");
            }
            return element.getAsJsonObject();
        }
    }

    private static byte[] compress(JsonElement value) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
             OutputStreamWriter writer = new OutputStreamWriter(gzipStream, StandardCharsets.UTF_8)) {
            GSON.toJson(value, writer);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode compressed JSON", e);
        }
        return byteStream.toByteArray();
    }
}
