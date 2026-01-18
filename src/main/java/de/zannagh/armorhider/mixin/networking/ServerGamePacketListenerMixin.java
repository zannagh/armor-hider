package de.zannagh.armorhider.mixin.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.PayloadRegistry;
import de.zannagh.armorhider.net.ServerConnectionEvents;
import de.zannagh.armorhider.net.ServerPayloadContext;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle custom payloads on the server side and player join events.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin extends ServerCommonPacketListenerImpl {

    @Shadow
    public ServerPlayer player;

    @Shadow
    public abstract ServerPlayer getPlayer();

    @Unique
    final MinecraftServer minecraftServer;
    
    
    public ServerGamePacketListenerMixin(MinecraftServer minecraftServer, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraftServer, connection, commonListenerCookie);
        this.minecraftServer = minecraftServer;
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
        private void handleCustomPayloadReceivedAsync(ServerboundCustomPayloadPacket packet, CallbackInfo callbackInfo) {
        CustomPacketPayload payload = packet.payload();
        var payloadId = payload.type().id();

        // Check if this is one of our registered payloads
        var handler = PayloadRegistry.getC2SHandler(payloadId);
        if (handler != null) {
            ArmorHider.LOGGER.debug("Handling C2S payload: {}", payloadId);

            var context = new ServerPayloadContext(getPlayer(), server);
            var handlerContext = new PayloadRegistry.PayloadHandlerContext<>(payload, context);

            try {
                handler.accept(handlerContext);
            } catch (Exception e) {
                ArmorHider.LOGGER.error("Error handling C2S payload: {}", payloadId, e);
            }
            callbackInfo.cancel();
        }
    }
}
