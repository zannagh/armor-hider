package de.zannagh.armorhider.networking;

import net.minecraft.client.Minecraft;
//? if >= 1.20.5 {
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
//? if < 1.20.5 {
/*import de.zannagh.armorhider.net.LegacyPacketHandler;
import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerWideSettings;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * Utility for sending packets from client to server without Fabric API.
 */
public final class ClientPacketSender {

    private ClientPacketSender() {
    }

    //? if >= 1.20.5 {
    public static void sendToServer(CustomPacketPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Cannot send packet: not connected to a server");
        }
        connection.send(new ServerboundCustomPayloadPacket(payload));
    }
    //?}

    //? if < 1.20.5 {
    /*public static void sendToServer(PlayerConfig config) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Cannot send packet: not connected to a server");
        }
        ResourceLocation channel = LegacyPacketHandler.getPlayerConfigChannel();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CompressedJsonCodec.encodeLegacy(config, buf);
        connection.send(new ServerboundCustomPayloadPacket(channel, buf));
    }

    public static void sendToServer(ServerWideSettings settings) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Cannot send packet: not connected to a server");
        }
        ResourceLocation channel = LegacyPacketHandler.getServerWideSettingsChannel();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CompressedJsonCodec.encodeLegacy(settings, buf);
        connection.send(new ServerboundCustomPayloadPacket(channel, buf));
    }
    *///?}
}
