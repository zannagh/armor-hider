package de.zannagh.armorhider.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side connection events.
 * Replaces Fabric API's ClientPlayConnectionEvents.
 */
public final class ClientConnectionEvents {

    /**
     * Functional interface for join handlers.
     */
    @FunctionalInterface
    public interface JoinHandler {
        void onJoin(ClientPacketListener handler, Minecraft client);
    }

    private static final List<JoinHandler> JOIN_HANDLERS = new ArrayList<>();

    /**
     * Register a handler for when the client joins a server and is ready to play.
     * This is equivalent to ClientPlayConnectionEvents.JOIN in Fabric API.
     */
    public static void registerJoin(JoinHandler handler) {
        JOIN_HANDLERS.add(handler);
    }

    /**
     * Called by the mixin when the client joins a server.
     * Do not call this directly.
     */
    public static void onClientJoin(ClientPacketListener handler, Minecraft client) {
        for (var h : JOIN_HANDLERS) {
            try {
                h.onJoin(handler, client);
            } catch (Exception e) {
                de.zannagh.armorhider.ArmorHider.LOGGER.error("Error in client join handler", e);
            }
        }
    }
}
