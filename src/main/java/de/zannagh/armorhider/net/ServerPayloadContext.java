package de.zannagh.armorhider.net;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Context for server-side payload handlers.
 * Provides access to the player and server instance.
 */
public record ServerPayloadContext(
        ServerPlayer player,
        MinecraftServer server
) {
}
