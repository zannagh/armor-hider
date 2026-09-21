package de.zannagh.armorhider.server;

import de.zannagh.armorhider.ArmorHider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ServerConnectionEvents {

    private static final List<BiConsumer<ServerPlayer, MinecraftServer>> JOIN_HANDLERS = new ArrayList<>();
    private static final Map<UUID, Long> RECENT_JOINS = new ConcurrentHashMap<>();
    private static final long DEDUPE_WINDOW_MS = 2000;

    public static void registerJoin(BiConsumer<ServerPlayer, MinecraftServer> handler) {
        JOIN_HANDLERS.add(handler);
    }

    /**
     * Fires the join event for a player that is <b>already in the player list</b>. Called from the
     * {@code PlayerList.placeNewPlayer} tail mixin, which is the first moment that is true.
     *
     * <p>There is deliberately no waiting here any more. This used to be raised from the login listener,
     * before the configuration phase had even started, and bridged the gap by polling the player list on
     * a pooled thread until an exponential backoff ran out at ~4.3 s. That window belongs to the client,
     * not to us, so on a real server the poll could simply lose - and losing it silently skipped every
     * handler, leaving the client with no config, no permission level and no shared rules. Hooking the
     * moment itself removes the race instead of widening it. See #375.</p>
     *
     * <p>The de-duplication below is kept as cheap insurance: {@code placeNewPlayer} is called once per
     * join by vanilla, but it is a public method and nothing stops another mod routing a respawn or a
     * transfer back through it.</p>
     */
    public static void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        UUID playerId = player.getUUID();

        long now = System.currentTimeMillis();
        // Evict entries older than the dedupe window before checking: they can never trigger a dedupe-skip
        // again, so keeping them would grow RECENT_JOINS by one permanent entry per unique UUID over the
        // server's lifetime. Joins are infrequent, so this sweep is cheap and bounds the map to the window.
        RECENT_JOINS.entrySet().removeIf(entry -> (now - entry.getValue()) >= DEDUPE_WINDOW_MS);
        Long lastJoin = RECENT_JOINS.get(playerId);
        if (lastJoin != null && (now - lastJoin) < DEDUPE_WINDOW_MS) {
            return;
        }
        RECENT_JOINS.put(playerId, now);

        invokeHandlers(player, server);
    }

    private static void invokeHandlers(ServerPlayer player, MinecraftServer server) {
        for (var handler : JOIN_HANDLERS) {
            try {
                handler.accept(player, server);
            } catch (Exception e) {
                ArmorHider.LOGGER.error("Error in player join handler", e);
            }
        }
    }
}
