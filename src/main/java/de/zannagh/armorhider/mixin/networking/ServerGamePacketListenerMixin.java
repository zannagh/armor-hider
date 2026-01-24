//? if >= 1.20.5 {
package de.zannagh.armorhider.mixin.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.PayloadRegistry;
import de.zannagh.armorhider.net.ServerPayloadContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mixin to handle custom payloads on the server side and player join events.
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin extends ServerCommonPacketListenerImpl {

    @Unique
    final MinecraftServer minecraftServer;
    @Shadow
    public ServerPlayer player;

    public ServerGamePacketListenerMixin(MinecraftServer minecraftServer, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraftServer, connection, commonListenerCookie);
        this.minecraftServer = minecraftServer;
    }

    @Shadow
    public abstract ServerPlayer getPlayer();

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void handleCustomPayloadReceivedAsync(ServerboundCustomPayloadPacket packet, CallbackInfo callbackInfo) {
        CustomPacketPayload payload = packet.payload();
        var payloadId = payload.type().id();

        var handler = PayloadRegistry.getC2SHandler(payloadId);
        if (handler == null) {
            return;
        }

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
//?}
