package de.zannagh.armorhider.mixin.client.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.PayloadRegistry;
import de.zannagh.armorhider.networking.ClientConnectionEvents;
import de.zannagh.armorhider.networking.ClientPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle custom payloads on the client side and player join events.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin  extends ClientCommonPacketListenerImpl {

    protected ClientPacketListenerMixin(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }

    /**
     * Handle incoming custom payloads from the server.
     */
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onHandleCustomPayload(CustomPacketPayload customPacketPayload, CallbackInfo ci) {
        var payloadId = customPacketPayload.type().id();

        // Check if this is one of our registered payloads
        var handler = PayloadRegistry.getS2CHandler(payloadId);
        if (handler != null) {
            ArmorHider.LOGGER.debug("Handling S2C payload: {}", payloadId);

            var context = new ClientPayloadContext((ClientPacketListener) (Object) this, minecraft);
            var handlerContext = new PayloadRegistry.PayloadHandlerContext<>(customPacketPayload, context);

            try {
                handler.accept(handlerContext);
            } catch (Exception e) {
                ArmorHider.LOGGER.error("Error handling S2C payload: {}", payloadId, e);
            }

            ci.cancel(); // Prevent vanilla from trying to handle it
        }
    }

    /**
     * Called when the client finishes the login process and is ready to play.
     * This is equivalent to ClientPlayConnectionEvents.JOIN in Fabric API.
     */
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onHandleLogin(CallbackInfo ci) {
        var listener = (ClientPacketListener) (Object) this;
        ArmorHider.LOGGER.info("Client joined server (client-side)");
        ClientConnectionEvents.onClientJoin(listener, minecraft);
    }
}
