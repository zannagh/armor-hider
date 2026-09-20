package de.zannagh.armorhider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.api.ArmorHiderApiImpl;
import de.zannagh.armorhider.api.ArmorHiderInitializer;
import de.zannagh.armorhider.log.EnrichedLogger;
import de.zannagh.armorhider.configuration.serialization.*;
import de.zannagh.armorhider.net.*;
import de.zannagh.armorhider.server.ServerLifecycleEvents;
import de.zannagh.armorhider.server.ServerRuntime;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.level.storage.LevelResource;

public class ArmorHider {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapterFactory(new ServerConfigurationDeserializer())
            .registerTypeAdapterFactory(new ConfigurationSourceSerializer())
            .registerTypeAdapterFactory(new ConfigurationItemSerializer())
            .create();
    public static final String MOD_ID = "armor-hider";
    public static final EnrichedLogger LOGGER = new EnrichedLogger(LoggerFactory.getLogger(MOD_ID));

    private static volatile ServerRuntime runtime = null;

    /**
     * Set once by {@link #useAsApiOnly()}, read from the client's UI and keybind paths. Volatile
     * because it is written from a consuming mod's initializer (the loader's thread) and read from the
     * client tick and render threads.
     */
    private static volatile boolean apiOnly = false;

    public static ServerRuntime getRuntime() {
        return runtime;
    }

    /**
     * Puts Armor Hider into <b>API-only mode</b>: a consuming mod drives it through
     * {@link de.zannagh.armorhider.api.ArmorHiderApi} and {@code ArmorHiderRenderApi}, and Armor Hider
     * stops presenting itself as a mod of its own.
     *
     * <p>Suppressed:
     * <ul>
     *   <li><b>Keybinds</b> - the toggle, open-settings and preset mappings are never added to
     *       {@code Options.keyMappings}, so they also stop appearing in the vanilla Controls screen.</li>
     *   <li><b>Settings screens and every entry point into them</b> - the Options-screen button, the
     *       Skin Customization panel, the ModMenu config factory and the open-settings keybind. The
     *       screen classes stay compiled and functional; nothing opens them any more.</li>
     * </ul>
     *
     * <p>Deliberately kept:
     * <ul>
     *   <li><b>Everything network</b> - the handshake, the player-config sync, combat-log relaying and
     *       the shared-rule transport behind {@code AhRenderRuleBuilder.shared()}.</li>
     *   <li><b>The render pipeline and the entire API</b>, rules and interceptors included.</li>
     *   <li><b>The end user's own configuration</b>: it still loads, persists, syncs and drives
     *       rendering - only the UI to change it is gone. An existing user's saved settings keep
     *       working and the broadcast {@code PlayerConfig} stays meaningful; a fresh install sits at
     *       vanilla defaults, so nothing hides unless the consuming mod's rules say so.</li>
     * </ul>
     *
     * <p>Call this as early as possible. A Fabric {@code ModInitializer}/{@code ClientModInitializer}
     * and a NeoForge mod constructor all run before {@code Options} is loaded, which is when the
     * keybinds would be installed. A later call still works - the mappings are stripped again on the
     * next client tick - it just means they existed briefly.
     *
     * <p>The switch is <b>one-way and idempotent</b>. There is deliberately no way back: a mod that has
     * already taken the UI away from its users cannot meaningfully hand it back mid-session, and a
     * reversible flag would need every read site to cope with the UI reappearing under it.
     *
     * @since 0.13.0
     */
    public static void useAsApiOnly() {
        if (apiOnly) {
            return;
        }
        apiOnly = true;
        LOGGER.info("Armor Hider switched to API-only mode: config screens and keybinds are suppressed, "
                + "networking and the render API stay active.");
    }

    /**
     * @return whether {@link #useAsApiOnly()} has been called.
     * @since 0.13.0
     */
    public static boolean isApiOnly() {
        return apiOnly;
    }


    public static void init() {
        LOGGER.info("Initializing...");

        SmokeMode.maybeArm();
        DevRunWatchdog.maybeArm();

        ArmorHiderApiImpl.init();
        // Resolve every eunomia payload with armor-hider's Gson (its config type adapters serialize the
        // PlayerConfig/ServerConfiguration/ServerWideSettings graphs). Installed before any packet flows.
        de.zannagh.eunomia.networking.serialization.NetworkSerializer.setGson(GSON);

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

        ArmorHiderServerNet.init();

        ArmorHiderInitializer.dispatchAll(ArmorHiderApi.getInstance());

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
