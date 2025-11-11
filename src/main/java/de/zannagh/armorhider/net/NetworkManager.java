// Network.java
package de.zannagh.armorhider.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.zannagh.armorhider.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

public final class NetworkManager {
    private static final Gson GSON = new GsonBuilder().create();

    public static void initServer() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var p = handler.player;
            var currentConfig = ServerRuntime.store.getConfig();
            sendToClient(p, currentConfig);
        });
        ServerPlayNetworking.registerGlobalReceiver(SettingsC2SPacket.IDENTIFIER, (payload, context) ->{
            var data = payload.config();
            try {
                ServerRuntime.put(data.playerId, data);
                sendToAllClients(ServerRuntime.store.getConfig());
            } catch(Exception e) {
                Armorhider.LOGGER.error("Failed to store player data!", e);
            }
        });
    }

    private static void sendToClient(ServerPlayerEntity player, List<PlayerConfig> cfg) {
        ServerPlayNetworking.send(player, new SettingsS2CPacket(cfg));
    }

    private static void sendToAllClients(List<PlayerConfig> cfg) {
        var players = ServerRuntime.server.getPlayerManager().getPlayerList();
        players.forEach(player -> {
            ServerPlayNetworking.send(player, new SettingsS2CPacket(cfg));
        });
    }
    
    public static final class ServerRuntime {
        public static ServerConfigStore store;
        public static MinecraftServer server;

        public static void init(MinecraftServer s) {
            server = s;
            store = ServerConfigStore.open();
        }
        public static PlayerConfig get(UUID id) { return store.get(id); }
        public static void put(UUID id, PlayerConfig c) { store.put(id, c); }
        public static void flushLater() { server.execute(store::save); }
    }
}