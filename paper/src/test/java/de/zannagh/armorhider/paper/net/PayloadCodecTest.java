package de.zannagh.armorhider.paper.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Wire-format contract for {@link PayloadCodec}. This decodes attacker-controlled bytes, so every
 * size and shape guard is exercised, not just the happy round-trip.
 */
@DisplayName("PayloadCodec framing and size guards")
class PayloadCodecTest {

    @Test
    @DisplayName("encode then decode round-trips a JSON object unchanged")
    void roundTrips() {
        JsonObject payload = new JsonObject();
        payload.addProperty("name", "ArmorHider");
        payload.addProperty("count", 42);
        payload.addProperty("flag", true);

        byte[] framed = PayloadCodec.encode(payload);
        JsonObject decoded = PayloadCodec.decodeServerbound(framed);

        assertEquals(payload, decoded);
    }

    @Test
    @DisplayName("framing is [int32 BE length][gzip payload]")
    void framePrefixIsBigEndianCompressedLength() {
        byte[] framed = PayloadCodec.encode(new JsonObject());
        int prefixed = ((framed[0] & 0xFF) << 24) | ((framed[1] & 0xFF) << 16)
                | ((framed[2] & 0xFF) << 8) | (framed[3] & 0xFF);
        assertEquals(framed.length - 4, prefixed, "length prefix must equal the compressed byte count");
    }

    @Test
    @DisplayName("a payload shorter than the 4-byte length prefix is rejected")
    void rejectsTruncatedFrame() {
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.decodeServerbound(new byte[]{0, 1, 2}));
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.decodeServerbound(null));
    }

    @Test
    @DisplayName("a length prefix larger than the readable body is rejected before allocating")
    void rejectsLengthExceedingBuffer() {
        byte[] framed = new byte[8];
        // Claim 0x7FFFFFFF compressed bytes while only 4 are readable.
        framed[0] = 0x7F;
        framed[1] = (byte) 0xFF;
        framed[2] = (byte) 0xFF;
        framed[3] = (byte) 0xFF;
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.decodeServerbound(framed));
    }

    @Test
    @DisplayName("a negative length prefix is rejected")
    void rejectsNegativeLength() {
        byte[] framed = new byte[8];
        framed[0] = (byte) 0x80; // sign bit set -> negative int
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.decodeServerbound(framed));
    }

    @Test
    @DisplayName("a serverbound payload above the serverbound ceiling is rejected")
    void rejectsOversizedServerboundLengthPrefix() throws IOException {
        // Well-formed frame whose declared length just exceeds the serverbound cap but stays within
        // the buffer, so only the serverbound guard can reject it.
        int declared = PayloadCodec.MAX_SERVERBOUND_PAYLOAD_BYTES + 1;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(out);
        data.writeInt(declared);
        data.write(new byte[declared]);
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.decodeServerbound(out.toByteArray()));
    }

    @Test
    @DisplayName("a valid gzip frame that decodes to a non-object is rejected")
    void rejectsNonObjectJson() {
        byte[] framed = frameGzippedUtf8("\"just a string\"");
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.decodeServerbound(framed));
    }

    @Test
    @DisplayName("encode rejects an object whose compressed form exceeds the clientbound ceiling")
    void encodeRejectsOversizedPayload() {
        JsonObject payload = new JsonObject();
        // Base64 of fixed-seed random bytes is genuinely incompressible, so gzip output stays above
        // MAX_PAYLOAD_BYTES rather than collapsing the way a repeated pattern would. Deterministic
        // via the seeded Random so the test is reproducible.
        byte[] entropy = new byte[PayloadCodec.MAX_PAYLOAD_BYTES * 2];
        new java.util.Random(0xA12043L).nextBytes(entropy);
        payload.add("blob", new JsonPrimitive(java.util.Base64.getEncoder().encodeToString(entropy)));
        assertThrows(IllegalStateException.class, () -> PayloadCodec.encode(payload));
    }

    private static byte[] frameGzippedUtf8(String json) {
        return frameGzippedRaw(json.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] frameGzippedRaw(byte[] raw) {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(raw);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        byte[] body = compressed.toByteArray();
        ByteArrayOutputStream framed = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(framed)) {
            out.writeInt(body.length);
            out.write(body);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return framed.toByteArray();
    }
}
