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

    private static final List<JoinHandler> JOIN_HANDLERS = new ArrayList<>();

    public static void registerJoin(JoinHandler handler) {
        JOIN_HANDLERS.add(handler);
    }

    public static void onClientJoin(ClientPacketListener handler, Minecraft client) {
        for (var h : JOIN_HANDLERS) {
            try {
                h.onJoin(handler, client);
            } catch (Exception e) {
                de.zannagh.armorhider.ArmorHider.LOGGER.error("Error in client join handler", e);
            }
        }
    }

    @FunctionalInterface
    public interface JoinHandler {
        void onJoin(ClientPacketListener handler, Minecraft client);
    }
}
