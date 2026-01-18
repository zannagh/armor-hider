package de.zannagh.armorhider.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registry for custom packet payloads without Fabric API.
 * Stores payload types and their codecs for both C2S and S2C directions.
 */
public final class PayloadRegistry {

    private static final Map<Identifier, PayloadEntry<?>> C2S_PAYLOADS = new HashMap<>();
    private static final Map<Identifier, PayloadEntry<?>> S2C_PAYLOADS = new HashMap<>();

    private static final Map<Identifier, Consumer<PayloadHandlerContext<?>>> C2S_HANDLERS = new HashMap<>();
    private static final Map<Identifier, Consumer<PayloadHandlerContext<?>>> S2C_HANDLERS = new HashMap<>();

    public record PayloadEntry<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super ByteBuf, T> codec
    ) {}

    public record PayloadHandlerContext<T extends CustomPacketPayload>(
            T payload,
            Object context
    ) {}

    /**
     * Register a C2S (client to server) payload type.
     */
    public static <T extends CustomPacketPayload> void registerC2S(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super ByteBuf, T> codec) {
        C2S_PAYLOADS.put(type.id(), new PayloadEntry<>(type, codec));
        ArmorHider.LOGGER.info("Registered C2S payload: {}", type.id());
    }

    /**
     * Register an S2C (server to client) payload type.
     */
    public static <T extends CustomPacketPayload> void registerS2C(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super ByteBuf, T> codec) {
        S2C_PAYLOADS.put(type.id(), new PayloadEntry<>(type, codec));
        ArmorHider.LOGGER.info("Registered S2C payload: {}", type.id());
    }

    /**
     * Register a handler for C2S payloads (called on server).
     */
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void registerC2SHandler(
            CustomPacketPayload.Type<T> type,
            Consumer<PayloadHandlerContext<T>> handler) {
        C2S_HANDLERS.put(type.id(), (Consumer<PayloadHandlerContext<?>>) (Consumer<?>) handler);
    }

    /**
     * Register a handler for S2C payloads (called on client).
     */
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void registerS2CHandler(
            CustomPacketPayload.Type<T> type,
            Consumer<PayloadHandlerContext<T>> handler) {
        S2C_HANDLERS.put(type.id(), (Consumer<PayloadHandlerContext<?>>) (Consumer<?>) handler);
    }

    public static PayloadEntry<?> getC2SPayload(Identifier id) {
        return C2S_PAYLOADS.get(id);
    }

    public static PayloadEntry<?> getS2CPayload(Identifier id) {
        return S2C_PAYLOADS.get(id);
    }

    public static Consumer<PayloadHandlerContext<?>> getC2SHandler(Identifier id) {
        return C2S_HANDLERS.get(id);
    }

    public static Consumer<PayloadHandlerContext<?>> getS2CHandler(Identifier id) {
        return S2C_HANDLERS.get(id);
    }

    public static Map<Identifier, PayloadEntry<?>> getAllC2S() {
        return C2S_PAYLOADS;
    }

    public static Map<Identifier, PayloadEntry<?>> getAllS2C() {
        return S2C_PAYLOADS;
    }

    public static boolean hasC2S(Identifier id) {
        return C2S_PAYLOADS.containsKey(id);
    }

    public static boolean hasS2C(Identifier id) {
        return S2C_PAYLOADS.containsKey(id);
    }

    /**
     * Initialize all payload registrations.
     * Called during mod initialization.
     */
    public static void init() {
        // Register C2S payloads (client -> server)
        registerC2S(PlayerConfig.TYPE, PlayerConfig.STREAM_CODEC);
        registerC2S(ServerWideSettings.TYPE, ServerWideSettings.STREAM_CODEC);

        // Register S2C payloads (server -> client)
        registerS2C(ServerConfiguration.TYPE, ServerConfiguration.STREAM_CODEC);

        ArmorHider.LOGGER.info("Payload registry initialized");
    }
}
