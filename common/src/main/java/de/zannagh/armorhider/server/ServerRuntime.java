package de.zannagh.armorhider.server;

import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.UUID;

public final class ServerRuntime {
    private final ServerConfigStore store;
    private final MinecraftServer server;
    /**
     * Live, unpersisted shared render-rule state. Tied to the runtime rather than to the config store
     * on purpose: it lasts exactly as long as the server does, and never reaches the world file.
     */
    private final SharedRuleStore sharedRules = new SharedRuleStore();

    public ServerRuntime(MinecraftServer server, Path configPath) {
        this.server = server;
        this.store = new ServerConfigStore(configPath);
    }

    public ServerConfigStore getStore() {
        return store;
    }

    public SharedRuleStore getSharedRules() {
        return sharedRules;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public void put(UUID id, PlayerConfig c) {
        store.put(id, c);
        store.saveCurrent();
    }
}
