package de.zannagh.armorhider.net;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Utility class for sending packets without Fabric API.
 */
public final class PacketSender {

    private PacketSender() {
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        player.connection.send(new ClientboundCustomPayloadPacket(payload));
    }
}
