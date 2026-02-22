//? if < 1.20.5 {
/*package de.zannagh.armorhider.mixin.client.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.LegacyPacketHandler;
import de.zannagh.armorhider.net.PayloadRegistry;
import de.zannagh.armorhider.networking.ClientConnectionEvents;
import de.zannagh.armorhider.networking.ClientPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mixin to handle custom payloads on the client side for 1.20.x (pre-1.20.5).
@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onHandleCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        ResourceLocation channel = packet.getIdentifier();
        FriendlyByteBuf data = packet.getData();

        // Try to get the decoder for this channel
        var decoder = LegacyPacketHandler.getDecoder(channel);
        if (decoder == null) {
            return; // Not our packet
        }

        // Decode the payload
        Object payload;
        try {
            payload = decoder.apply(data);
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to decode S2C payload from channel: {}", channel, e);
            return;
        }

        ArmorHider.LOGGER.debug("Received S2C payload from channel: {}", channel);

        var context = new ClientPayloadContext((ClientPacketListener) (Object) this, minecraft);
        var handlerContext = new PayloadRegistry.PayloadHandlerContext<>(payload, context);

        // Get and call the handler
        var handler = LegacyPacketHandler.getS2CHandler(channel);
        if (handler != null) {
            try {
                handler.accept(handlerContext);
            } catch (Exception e) {
                ArmorHider.LOGGER.error("Error handling S2C payload: {}", channel, e);
            }
        }

        ci.cancel(); // Prevent vanilla from trying to handle it
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onHandleLogin(CallbackInfo ci) {
        var listener = (ClientPacketListener) (Object) this;
        ArmorHider.LOGGER.info("Client joined server (client-side)");
        ClientConnectionEvents.onClientJoin(listener, minecraft);
    }
}
*///?}
