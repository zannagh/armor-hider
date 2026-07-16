package de.zannagh.armorhider.net;

//? if >= 1.20.5 {
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
//? if < 1.20.5 {
/*import de.zannagh.armorhider.net.CompressedJsonCodec;
import de.zannagh.armorhider.net.packets.PermissionPacket;
import de.zannagh.armorhider.net.packets.CombatLogNotificationPacket;
import de.zannagh.armorhider.server.ServerConfiguration;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
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
        Identifier channel = LegacyPacketHandler.getServerConfigChannel();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CompressedJsonCodec.encodeLegacy(config, buf);
        player.connection.send(new ClientboundCustomPayloadPacket(channel, buf));
    }

    public static void sendToPlayer(ServerPlayer player, PermissionPacket permissions) {
        Identifier channel = LegacyPacketHandler.getPermissionChannel();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CompressedJsonCodec.encodeLegacy(permissions, buf);
        player.connection.send(new ClientboundCustomPayloadPacket(channel, buf));
    }
    
    public static void sendToPlayer(ServerPlayer player, CombatLogNotificationPacket combatLogNotification) {
        Identifier channel = LegacyPacketHandler.getCombatLogNotificationChannel();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CompressedJsonCodec.encodeLegacy(combatLogNotification, buf);
        player.connection.send(new ClientboundCustomPayloadPacket(channel, buf));
    }
    *///?}
}
