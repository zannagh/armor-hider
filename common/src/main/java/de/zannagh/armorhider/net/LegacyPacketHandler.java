//? if < 1.20.5 {
/*package de.zannagh.armorhider.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.server.*;
import de.zannagh.armorhider.net.packets.*;
import de.zannagh.armorhider.net.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

// Handles packet encoding/decoding for 1.20.x versions (pre-1.20.5).
// In these versions, there is no CustomPacketPayload interface or StreamCodec.
public final class LegacyPacketHandler {

    private static final Identifier PLAYER_CONFIG_CHANNEL = new Identifier("armorhider", "settings_c2s_packet");
    private static final Identifier SERVER_WIDE_SETTINGS_CHANNEL = new Identifier("armorhider", "server_wide_settings");
    private static final Identifier SERVER_CONFIG_CHANNEL = new Identifier("armorhider", "settings_s2c_packet");
    private static final Identifier PERMISSION_CHANNEL = new Identifier("armorhider", "permissions_s2c_packet");
    private static final Identifier COMBAT_LOG_EVENT_CHANNEL = new Identifier("armorhider", "combatlog_c2s_packet");
    private static final Identifier COMBAT_LOG_NOTIFICATION_CHANNEL = new Identifier("armorhider", "combatlog_s2c_packet");

    private static final Map<Identifier, Function<FriendlyByteBuf, Object>> DECODERS = new HashMap<>();
    private static final Map<Identifier, BiConsumer<Object, FriendlyByteBuf>> ENCODERS = new HashMap<>();
    private static final Map<Identifier, java.util.function.Consumer<PayloadRegistry.PayloadHandlerContext<?>>> C2S_HANDLERS = new HashMap<>();
    private static final Map<Identifier, java.util.function.Consumer<PayloadRegistry.PayloadHandlerContext<?>>> S2C_HANDLERS = new HashMap<>();

    static {
        // Register decoders for C2S packets
        DECODERS.put(PLAYER_CONFIG_CHANNEL, buf -> CompressedJsonCodec.decodeLegacy(buf, PlayerConfig.class));
        DECODERS.put(SERVER_WIDE_SETTINGS_CHANNEL, buf -> CompressedJsonCodec.decodeLegacy(buf, ServerWideSettings.class));
        DECODERS.put(COMBAT_LOG_EVENT_CHANNEL, buf -> CompressedJsonCodec.decodeLegacy(buf, CombatLogEventPacket.class));
        DECODERS.put(COMBAT_LOG_NOTIFICATION_CHANNEL, buf -> CompressedJsonCodec.decodeLegacy(buf, CombatLogNotificationPacket.class));

        // Register decoders for S2C packets
        DECODERS.put(SERVER_CONFIG_CHANNEL, buf -> CompressedJsonCodec.decodeLegacy(buf, ServerConfiguration.class));
        DECODERS.put(PERMISSION_CHANNEL, buf -> CompressedJsonCodec.decodeLegacy(buf, de.zannagh.armorhider.net.packets.PermissionPacket.class));

        // Register encoders
        ENCODERS.put(PLAYER_CONFIG_CHANNEL, (obj, buf) -> CompressedJsonCodec.encodeLegacy((PlayerConfig) obj, buf));
        ENCODERS.put(SERVER_WIDE_SETTINGS_CHANNEL, (obj, buf) -> CompressedJsonCodec.encodeLegacy((ServerWideSettings) obj, buf));
        ENCODERS.put(SERVER_CONFIG_CHANNEL, (obj, buf) -> CompressedJsonCodec.encodeLegacy((ServerConfiguration) obj, buf));
        ENCODERS.put(PERMISSION_CHANNEL, (obj, buf) -> CompressedJsonCodec.encodeLegacy((de.zannagh.armorhider.net.packets.PermissionPacket) obj, buf));
        ENCODERS.put(COMBAT_LOG_EVENT_CHANNEL, (obj, buf) -> CompressedJsonCodec.encodeLegacy((CombatLogEventPacket) obj, buf));
        ENCODERS.put(COMBAT_LOG_NOTIFICATION_CHANNEL, (obj, buf) -> CompressedJsonCodec.encodeLegacy((CombatLogNotificationPacket) obj, buf));
    }

    public static void registerC2SHandler(Identifier channel, java.util.function.Consumer<PayloadRegistry.PayloadHandlerContext<?>> handler) {
        C2S_HANDLERS.put(channel, handler);
    }

    public static void registerS2CHandler(Identifier channel, java.util.function.Consumer<PayloadRegistry.PayloadHandlerContext<?>> handler) {
        S2C_HANDLERS.put(channel, handler);
    }

    public static java.util.function.Consumer<PayloadRegistry.PayloadHandlerContext<?>> getS2CHandler(Identifier channel) {
        return S2C_HANDLERS.get(channel);
    }

    public static Function<FriendlyByteBuf, Object> getDecoder(Identifier channel) {
        return DECODERS.get(channel);
    }

    // Handles a C2S packet received from the client.
    // Returns true if the packet was handled, false otherwise.
    public static boolean handleC2SPacket(Identifier channel, FriendlyByteBuf data, ServerPayloadContext context) {
        var decoder = DECODERS.get(channel);
        if (decoder == null) {
            return false;
        }

        var handler = C2S_HANDLERS.get(channel);
        if (handler == null) {
            ArmorHider.LOGGER.debug("No handler registered for channel: {}", channel);
            return false;
        }

        try {
            Object payload = decoder.apply(data);
            ArmorHider.LOGGER.debug("Decoded C2S payload from channel: {}", channel);

            var handlerContext = new PayloadRegistry.PayloadHandlerContext<>(payload, context);
            handler.accept(handlerContext);
            return true;
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Error handling C2S packet from channel: {}", channel, e);
            return false;
        }
    }

    public static Identifier getPlayerConfigChannel() {
        return PLAYER_CONFIG_CHANNEL;
    }

    public static Identifier getServerWideSettingsChannel() {
        return SERVER_WIDE_SETTINGS_CHANNEL;
    }

    public static Identifier getServerConfigChannel() {
        return SERVER_CONFIG_CHANNEL;
    }

    public static Identifier getPermissionChannel() {
        return PERMISSION_CHANNEL;
    }
    
    public static Identifier getCombatLogEventChannel() {
        return COMBAT_LOG_EVENT_CHANNEL;
    }
    
    public static Identifier getCombatLogNotificationChannel() {
        return COMBAT_LOG_NOTIFICATION_CHANNEL;
    }

    public static void encode(Identifier channel, Object payload, FriendlyByteBuf buf) {
        var encoder = ENCODERS.get(channel);
        if (encoder != null) {
            encoder.accept(payload, buf);
        }
    }
}
*///?}
