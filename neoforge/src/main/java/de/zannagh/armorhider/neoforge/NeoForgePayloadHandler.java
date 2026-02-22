package de.zannagh.armorhider.neoforge;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.PayloadRegistry;
import de.zannagh.armorhider.net.ServerPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers all custom payloads with NeoForge's native payload system,
 * bridging NeoForge's {@link IPayloadContext} to the mod's
 * {@link PayloadRegistry.PayloadHandlerContext}.
 */
final class NeoForgePayloadHandler {

    static void registerAll(PayloadRegistrar registrar) {
        for (var entry : PayloadRegistry.getAllC2S().values()) {
            registerC2S(registrar, entry);
        }
        for (var entry : PayloadRegistry.getAllS2C().values()) {
            registerS2C(registrar, entry);
        }
    }

    private static <T extends CustomPacketPayload> void registerC2S(
            PayloadRegistrar registrar, PayloadRegistry.PayloadEntry<T> entry) {
        registrar.playToServer(entry.type(), entry.codec(), (payload, ctx) -> {
            var handler = PayloadRegistry.getC2SHandler(payload.type().id());
            if (handler == null) return;

            ServerPlayer player = (ServerPlayer) ctx.player();
            var serverCtx = new ServerPayloadContext(player, player.level().getServer());
            handler.accept(new PayloadRegistry.PayloadHandlerContext<>(payload, serverCtx));
        });
        ArmorHider.LOGGER.info("Registered NeoForge C2S payload: {}", entry.type().id());
    }

    private static <T extends CustomPacketPayload> void registerS2C(
            PayloadRegistrar registrar, PayloadRegistry.PayloadEntry<T> entry) {
        // Handler delegates to NeoForgeClientPayloadHandler to avoid loading client classes on the server
        registrar.playToClient(entry.type(), entry.codec(), NeoForgeClientPayloadHandler::handle);
        ArmorHider.LOGGER.info("Registered NeoForge S2C payload: {}", entry.type().id());
    }
}
