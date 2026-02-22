package de.zannagh.armorhider.neoforge;

import de.zannagh.armorhider.net.PayloadRegistry;
import de.zannagh.armorhider.networking.ClientPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-side S2C payload handler for NeoForge.
 * Separated from {@link NeoForgePayloadHandler} to avoid loading client classes on the server.
 */
final class NeoForgeClientPayloadHandler {

    static <T extends CustomPacketPayload> void handle(T payload, IPayloadContext ctx) {
        var handler = PayloadRegistry.getS2CHandler(payload.type().id());
        if (handler == null) return;

        var clientCtx = new ClientPayloadContext(
                (ClientPacketListener) ctx.listener(),
                Minecraft.getInstance()
        );
        handler.accept(new PayloadRegistry.PayloadHandlerContext<>(payload, clientCtx));
    }
}
