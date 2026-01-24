package de.zannagh.armorhider.net;

//? if >= 1.20.5 {
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
//? if < 1.20.5 {
/*import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import de.zannagh.armorhider.netPackets.PermissionPacket;
import de.zannagh.armorhider.resources.ServerConfiguration;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.server.level.ServerPlayer;

/**
 * Utility class for sending packets without Fabric API.
 */
public final class PacketSender {

    private PacketSender() {
    }

    //? if >= 1.20.5 {
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        player.connection.send(new ClientboundCustomPayloadPacket(payload));
    }
    //?}

    //? if < 1.20.5 {
    /*public static void sendToPlayer(ServerPlayer player, ServerConfiguration config) {
        ResourceLocation channel = LegacyPacketHandler.getServerConfigChannel();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CompressedJsonCodec.encodeLegacy(config, buf);
        player.connection.send(new ClientboundCustomPayloadPacket(channel, buf));
    }

    public static void sendToPlayer(ServerPlayer player, PermissionPacket permissions) {
        ResourceLocation channel = LegacyPacketHandler.getPermissionChannel();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CompressedJsonCodec.encodeLegacy(permissions, buf);
        player.connection.send(new ClientboundCustomPayloadPacket(channel, buf));
    }
    *///?}
}
