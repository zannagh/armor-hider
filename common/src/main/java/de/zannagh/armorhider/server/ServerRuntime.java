package de.zannagh.armorhider.server;

import de.zannagh.armorhider.server.packets.PlayerConfig;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.UUID;

public final class ServerRuntime {
    private final ServerConfigStore store;
    private final MinecraftServer server;

    public ServerRuntime(MinecraftServer server, Path configPath) {
        this.server = server;
        this.store = new ServerConfigStore(configPath);
    }

    public ServerConfigStore getStore() {
        return store;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public void put(UUID id, PlayerConfig c) {
        store.put(id, c);
        store.saveCurrent();
    }
}
