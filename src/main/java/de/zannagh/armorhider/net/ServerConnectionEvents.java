package de.zannagh.armorhider.net;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ServerConnectionEvents {

    private static final List<BiConsumer<ServerPlayer, MinecraftServer>> JOIN_HANDLERS = new ArrayList<>();
    private static final Map<UUID, Long> RECENT_JOINS = new ConcurrentHashMap<>();
    private static final long PLAYER_WAIT_TIMEOUT_MS = 5000;
    private static final long POLL_INTERVAL_MS = 50;
    private static final long DEDUPE_WINDOW_MS = 2000;

    public static void registerJoin(BiConsumer<ServerPlayer, MinecraftServer> handler) {
        JOIN_HANDLERS.add(handler);
    }

    public static void onPlayerJoin(GameProfile profile, MinecraftServer server) {
        UUID playerId = profile.id();
        String playerName = profile.name();

        long now = System.currentTimeMillis();
        Long lastJoin = RECENT_JOINS.get(playerId);
        if (lastJoin != null && (now - lastJoin) < DEDUPE_WINDOW_MS) {
            return;
        }
        RECENT_JOINS.put(playerId, now);

        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            ServerPlayer player = null;

            while (System.currentTimeMillis() - startTime < PLAYER_WAIT_TIMEOUT_MS) {
                player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    break;
                }
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (player == null) {
                de.zannagh.armorhider.ArmorHider.LOGGER.warn(
                    "Timed out waiting for player {} ({}) to appear in player list",
                    playerName, playerId
                );
                return;
            }

            final ServerPlayer foundPlayer = player;
            server.execute(() -> invokeHandlers(foundPlayer, server));
        });
    }

    public static void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();
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
                de.zannagh.armorhider.ArmorHider.LOGGER.error("Error in player join handler", e);
            }
        }
    }
}
