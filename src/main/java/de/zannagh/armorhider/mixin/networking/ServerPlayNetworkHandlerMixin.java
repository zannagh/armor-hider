//? if < 1.20.5 {
/*package de.zannagh.armorhider.mixin.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.LegacyPacketHandler;
import de.zannagh.armorhider.net.ServerPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mixin to handle custom payloads on the server side for 1.20.x (pre-1.20.5).
// In 1.20.x, custom payloads use ResourceLocation channel + FriendlyByteBuf data directly.
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayer player;

    @Final
    @Shadow
    private MinecraftServer server;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void handleCustomPayloadReceivedAsync(ServerboundCustomPayloadPacket packet, CallbackInfo callbackInfo) {
        ResourceLocation channel = packet.getIdentifier();
        FriendlyByteBuf data = packet.getData();

        var context = new ServerPayloadContext(player, server);

        if (LegacyPacketHandler.handleC2SPacket(channel, data, context)) {
            callbackInfo.cancel();
        }
    }
}
*///?}
