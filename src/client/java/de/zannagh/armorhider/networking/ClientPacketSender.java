package de.zannagh.armorhider.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Utility class for sending packets from the client without Fabric API.
 */
public final class ClientPacketSender {

    private ClientPacketSender() {}

    /**
     * Send a custom payload to the server.
     * This replaces ClientPlayNetworking.send().
     *
     * @param payload The payload to send
     * @throws IllegalStateException if not connected to a server
     */
    public static void sendToServer(CustomPacketPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Cannot send packet: not connected to a server");
        }
        connection.send(new ServerboundCustomPayloadPacket(payload));
    }

    /**
     * Check if the client is connected to a server.
     *
     * @return true if connected
     */
    public static boolean isConnected() {
        return Minecraft.getInstance().getConnection() != null;
    }
}
