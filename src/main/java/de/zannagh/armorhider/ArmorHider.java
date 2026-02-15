package de.zannagh.armorhider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.zannagh.armorhider.common.util.EnrichedLogger;
import de.zannagh.armorhider.common.configuration.ConfigurationItemSerializer;
import de.zannagh.armorhider.configuration.ConfigurationSourceSerializer;
import de.zannagh.armorhider.configuration.ServerConfigurationDeserializer;
import de.zannagh.armorhider.net.CommsManager;
import de.zannagh.armorhider.net.PayloadRegistry;
import de.zannagh.armorhider.net.ServerLifecycleEvents;
import de.zannagh.armorhider.net.ServerRuntime;
import net.fabricmc.api.ModInitializer;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.level.storage.LevelResource;

public class ArmorHider implements ModInitializer {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapterFactory(new ServerConfigurationDeserializer())
            .registerTypeAdapterFactory(new ConfigurationSourceSerializer())
            .registerTypeAdapterFactory(new ConfigurationItemSerializer())
            .create();
    public static final String MOD_ID = "armor-hider";
    public static final EnrichedLogger LOGGER = new EnrichedLogger(LoggerFactory.getLogger(MOD_ID));

    private static volatile ServerRuntime runtime = null;

    public static ServerRuntime getRuntime() {
        return runtime;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing...");

        // Register server lifecycle events
        ServerLifecycleEvents.registerStarting(server -> {
            Path worldConfigPath = getWorldConfigPath(server);
            migrateGlobalConfigIfNeeded(worldConfigPath);
            runtime = new ServerRuntime(server, worldConfigPath);
            LOGGER.info("Server config store opened");
        });
        ServerLifecycleEvents.registerStopping(server -> {
            if (runtime != null) {
                runtime.getStore().saveCurrent();
            }
            runtime = null;
        });

        PayloadRegistry.init();
        CommsManager.initServer();
        LOGGER.info("Initialized!");
    }

    private static Path getWorldConfigPath(net.minecraft.server.MinecraftServer server) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        return worldDir.resolve("armor-hider.json");
    }

    private static void migrateGlobalConfigIfNeeded(Path worldConfigPath) {
        Path globalConfig = new File("config", "armor-hider-server.json").toPath();
        if (Files.exists(globalConfig) && !Files.exists(worldConfigPath)) {
            try {
                Files.createDirectories(worldConfigPath.getParent());
                Files.copy(globalConfig, worldConfigPath);
                LOGGER.info("Migrated global config to world: {}", worldConfigPath);
            } catch (IOException e) {
                LOGGER.error("Failed to migrate config", e);
            }
        }
    }
}
