//? if >= 1.20.5 {
package de.zannagh.armorhider.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.netPackets.PermissionPacket;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

//? if >= 1.21.11 {
import net.minecraft.resources.Identifier;
 //?}
//? if >= 1.20.5 && < 1.21.11 {
/*import net.minecraft.resources.ResourceLocation;
*///?}

public final class PayloadRegistry {

    //? if >= 1.21.11 {
    private static final Map<Identifier, PayloadEntry<?>> C2S_PAYLOADS = new HashMap<>();
    private static final Map<Identifier, PayloadEntry<?>> S2C_PAYLOADS = new HashMap<>();

    private static final Map<Identifier, Consumer<PayloadHandlerContext<?>>> C2S_HANDLERS = new HashMap<>();
    private static final Map<Identifier, Consumer<PayloadHandlerContext<?>>> S2C_HANDLERS = new HashMap<>();
    //?}
    //? if >= 1.20.5 && < 1.21.11 {
    /*private static final Map<ResourceLocation, PayloadEntry<?>> C2S_PAYLOADS = new HashMap<>();
    private static final Map<ResourceLocation, PayloadEntry<?>> S2C_PAYLOADS = new HashMap<>();

    private static final Map<ResourceLocation, Consumer<PayloadHandlerContext<?>>> C2S_HANDLERS = new HashMap<>();
    private static final Map<ResourceLocation, Consumer<PayloadHandlerContext<?>>> S2C_HANDLERS = new HashMap<>();
    *///?}

    public static void init() {
        registerC2S(PlayerConfig.TYPE, PlayerConfig.STREAM_CODEC);
        registerC2S(ServerWideSettings.TYPE, ServerWideSettings.STREAM_CODEC);
        registerS2C(ServerConfiguration.TYPE, ServerConfiguration.STREAM_CODEC);
        registerS2C(PermissionPacket.TYPE, PermissionPacket.STREAM_CODEC);
    }

    // Register a C2S (client to server) payload type.
    public static <T extends CustomPacketPayload> void registerC2S(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super ByteBuf, T> codec) {
        C2S_PAYLOADS.put(type.id(), new PayloadEntry<>(type, codec));
        ArmorHider.LOGGER.info("Registered C2S payload: {}", type.id());
    }

    // Register an S2C (server to client) payload type.
    public static <T extends CustomPacketPayload> void registerS2C(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super ByteBuf, T> codec) {
        S2C_PAYLOADS.put(type.id(), new PayloadEntry<>(type, codec));
        ArmorHider.LOGGER.info("Registered S2C payload: {}", type.id());
    }

    // Register a handler for C2S payloads (called on server).
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void registerC2SHandler(
            CustomPacketPayload.Type<T> type,
            Consumer<PayloadHandlerContext<T>> handler) {
        C2S_HANDLERS.put(type.id(), (Consumer<PayloadHandlerContext<?>>) (Consumer<?>) handler);
    }

    // Register a handler for S2C payloads (called on client).
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void registerS2CHandler(
            CustomPacketPayload.Type<T> type,
            Consumer<PayloadHandlerContext<T>> handler) {
        S2C_HANDLERS.put(type.id(), (Consumer<PayloadHandlerContext<?>>) (Consumer<?>) handler);
    }

    //? if >= 1.21.11 {
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
    //?}

    //? if >= 1.20.5 && < 1.21.11 {
    /*public static Consumer<PayloadHandlerContext<?>> getC2SHandler(ResourceLocation id) {
        return C2S_HANDLERS.get(id);
    }

    public static Consumer<PayloadHandlerContext<?>> getS2CHandler(ResourceLocation id) {
        return S2C_HANDLERS.get(id);
    }

    public static Map<ResourceLocation, PayloadEntry<?>> getAllC2S() {
        return C2S_PAYLOADS;
    }

    public static Map<ResourceLocation, PayloadEntry<?>> getAllS2C() {
        return S2C_PAYLOADS;
    }
    *///?}
    

    public record PayloadEntry<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super ByteBuf, T> codec
    ) {
    }

    public record PayloadHandlerContext<T>(
            T payload,
            Object context
    ) {
    }
}
//?}

//? if < 1.20.5 {
/*package de.zannagh.armorhider.net;

// Minimal stub for 1.20.x - the actual payload handling is done by LegacyPacketHandler
public final class PayloadRegistry {

    public static void init() {
        // No-op for 1.20.x - LegacyPacketHandler handles everything
    }

    // Context record used by both legacy and modern networking
    public record PayloadHandlerContext<T>(
            T payload,
            Object context
    ) {
    }
}
*///?}
