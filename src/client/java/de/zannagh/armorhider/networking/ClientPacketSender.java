package de.zannagh.armorhider.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Utility for sending packets from client to server without Fabric API.
 */
public final class ClientPacketSender {

    private ClientPacketSender() {
    }

    public static void sendToServer(CustomPacketPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Cannot send packet: not connected to a server");
        }
        connection.send(new ServerboundCustomPayloadPacket(payload));
    }
}
