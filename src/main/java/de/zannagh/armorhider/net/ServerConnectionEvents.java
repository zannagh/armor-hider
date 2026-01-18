package de.zannagh.armorhider.net;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Server-side connection events.
 * Replaces Fabric API's ServerPlayConnectionEvents.
 */
public final class ServerConnectionEvents {

    private static final List<BiConsumer<ServerPlayer, MinecraftServer>> JOIN_HANDLERS = new ArrayList<>();

    /**
     * Register a handler for when a player joins and starts playing.
     * This is equivalent to ServerPlayConnectionEvents.JOIN in Fabric API.
     */
    public static void registerJoin(BiConsumer<ServerPlayer, MinecraftServer> handler) {
        JOIN_HANDLERS.add(handler);
    }

    /**
     * Called by the mixin when a player joins.
     * Do not call this directly.
     */
    public static void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        for (var handler : JOIN_HANDLERS) {
            try {
                handler.accept(player, server);
            } catch (Exception e) {
                de.zannagh.armorhider.ArmorHider.LOGGER.error("Error in player join handler", e);
            }
        }
    }
}
