package de.zannagh.armorhider.net;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Server lifecycle events.
 * Replaces Fabric API's ServerLifecycleEvents.
 */
public final class ServerLifecycleEvents {

    private static final List<Consumer<MinecraftServer>> STARTING_HANDLERS = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> STOPPING_HANDLERS = new ArrayList<>();

    /**
     * Register a handler for when the server is starting.
     * This is equivalent to ServerLifecycleEvents.SERVER_STARTING in Fabric API.
     */
    public static void registerStarting(Consumer<MinecraftServer> handler) {
        STARTING_HANDLERS.add(handler);
    }

    /**
     * Register a handler for when the server is stopping.
     * This is equivalent to ServerLifecycleEvents.SERVER_STOPPING in Fabric API.
     */
    public static void registerStopping(Consumer<MinecraftServer> handler) {
        STOPPING_HANDLERS.add(handler);
    }

    /**
     * Called by the mixin when the server is starting.
     * Do not call this directly.
     */
    public static void onServerStarting(MinecraftServer server) {
        for (var handler : STARTING_HANDLERS) {
            try {
                handler.accept(server);
            } catch (Exception e) {
                de.zannagh.armorhider.ArmorHider.LOGGER.error("Error in server starting handler", e);
            }
        }
    }

    /**
     * Called by the mixin when the server is stopping.
     * Do not call this directly.
     */
    public static void onServerStopping(MinecraftServer server) {
        for (var handler : STOPPING_HANDLERS) {
            try {
                handler.accept(server);
            } catch (Exception e) {
                de.zannagh.armorhider.ArmorHider.LOGGER.error("Error in server stopping handler", e);
            }
        }
    }
}
