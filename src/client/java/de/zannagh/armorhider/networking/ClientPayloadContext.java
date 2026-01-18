package de.zannagh.armorhider.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Context for client-side payload handlers.
 * Provides access to the client instance and packet listener.
 */
public record ClientPayloadContext(
        ClientPacketListener handler,
        Minecraft client
) {
}
