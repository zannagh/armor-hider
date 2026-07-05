package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.CompatFlags;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.api.AhRenderModificationApi;
import de.zannagh.armorhider.client.api.AhRenderTypeFactory;
import de.zannagh.armorhider.client.api.impl.AhRendererRegistryImpl;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.compat.CompatManager;
import de.zannagh.armorhider.client.net.ClientCommunicationManager;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.client.render.rendertype.RenderTypeFactory;
import de.zannagh.armorhider.configuration.PresetManager;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import oshi.util.tuples.Pair;

public class ArmorHiderClient {

    public static int permissionLevel = 0; // Default to lowest.
    public static ClientConfigManager CLIENT_CONFIG_MANAGER = new ClientConfigManager();
    public static PresetManager PRESET_MANAGER = new PresetManager();

    public static final boolean FA_LOADED = CompatFlags.FA_LOADED || classExists("net.kenddie.fantasyarmor.FantasyArmor");
    public static final boolean GECKOLIB_LOADED = CompatFlags.GECKOLIB_LOADED || classExists("com.geckolib.renderer.GeoArmorRenderer");
    public static final boolean ET_LOADED = CompatFlags.ET_LOADED || classExists("dev.kikugie.elytratrims.ep.ETClientEntrypoint");
    public static final boolean EMF_LOADED = CompatFlags.EMF_LOADED || classExists("traben.entity_model_features.EMFManager");
    public static final boolean IRIS_LOADED = classExists("net.irisshaders.iris.api.v0.IrisApi");
    public static final boolean FIGURA_LOADED = CompatFlags.FIGURA_LOADED || classExists("org.figuramc.figura.FiguraMod");

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, ArmorHiderClient.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * A static initializer for client-related methods (communication, payloads, etc.).
     * Required so it can be called from loader-specific client-side mod implementations.
     */
    @SuppressWarnings("unused")
    public static void init() {
        ArmorHider.LOGGER.info("Armor Hider client initializing...");
        ClientCommunicationManager.initClient();

        initRenderTypes();

        ArmorHider.LOGGER.info("Registering render interceptors...");
        for (var interceptor : AhRendererRegistryImpl.getDefaultInterceptors()) {
            ArmorHider.LOGGER.info("Registering interceptor: {}", interceptor.getClass().getName());
            AhRenderInterceptionRegistryApi.register(interceptor, AhRendererRegistryImpl.DEFAULT_PRIORITY);
        }
        AhRenderInterceptionRegistryApi.suppressRenderInterceptionConditionallyForCarrier(RenderScope.ALL,
                (input) -> {
                    RenderScope scope = input.getFirst().getFirst();
                    IdentityCarrier carrier = input.getFirst().getSecond();
                    var render = input.getSecond();
                    var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(carrier.armorHider$playerName());
                    boolean isInvisible = carrier.armorHider$isPlayerInvisible();
                    if (!isInvisible) {
                        return false;
                    }

                    var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
                    if (serverConfig != null) {
                        var shouldSuppressByServer = (Boolean) serverConfig.serverWideSettings.disableArmorHiderOnInvisibilityGlobally.getValue();
                        if (shouldSuppressByServer) {
                            return true;
                        }
                    }
                    return (Boolean) config.disableArmorHiderOnInvisibility.getValue();
                });
        ArmorHider.LOGGER.info("Registered render interceptors.");

        ArmorHider.LOGGER.info("Setting up compatibilities...");
        CompatManager.init();
        ArmorHider.LOGGER.info("Compatibilities set up.");
    }

    /**
     * Mixin to this method to adjust render types or use {@link AhRenderModificationApi#registerRenderTypeFactory(AhRenderTypeFactory, int)} to change render type behavior.
     */
    public static void initRenderTypes() {
        AhRenderModificationApi.registerRenderTypeFactory(new RenderTypeFactory(), AhRenderModificationApi.getDefaultPriority());
    }

    public static String getCurrentPlayerName() {
        String name = PlayerNameUtil.getPlayerName(Minecraft.getInstance().player);
        return name != null ? name : ClientConfigManager.DEFAULT_PLAYER_NAME;
    }

    @Contract("_ -> new")
    public static @NonNull Pair<Boolean, PlayerInfo> isPlayerRemotePlayer(String playerName) {
        ClientPacketListener networkHandler = Minecraft.getInstance().getConnection();
        if (networkHandler == null) {
            return new Pair<>(false, null);
        }
        //? if >= 1.21.9
        PlayerInfo entry = networkHandler.getPlayerInfoIgnoreCase(playerName);
        //? if < 1.21.9
        //PlayerInfo entry = networkHandler.getPlayerInfo(playerName);
        if (entry == null) {
            return new Pair<>(false, null);
        }
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return new Pair<>(false, null);
        }
        //? if >= 1.21.9 {
        boolean isLocal = entry.getProfile().id().equals(localPlayer.getUUID())
                || playerName.equals(entry.getProfile().name());
        //?}
        //? if < 1.21.9 {
        /*boolean isLocal = entry.getProfile().getId().equals(localPlayer.getUUID())
                || playerName.equals(entry.getProfile().getName());
        *///?}
        return new Pair<>(!isLocal, entry);
    }
    
    public static void toggleDebugLogging() {
        if (DebugLogger.isEnabled()) {
            disableDebugLogging();
        } else {
            enableDebugLogging();
        }
    }
    
    private static void enableDebugLogging() {
        DebugLogger.enable();
        DebugLogger.log("Started debug logging.");

        StringBuilder config = new StringBuilder();
        config.append("--- Mod configuration ---\n");
        config.append("Local: ").append(ArmorHiderClient.CLIENT_CONFIG_MANAGER.local().toJson()).append("\n");
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig() != null) {
            config.append("Server: ").append(ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig().toJson()).append("\n");
        } else {
            config.append("Server: null\n");
        }
        DebugLogger.writeConfig(config.toString());
    }

    private static void disableDebugLogging() {
        DebugLogger.log("Stopped debug logging due to client request.");
        DebugLogger.disable();
    }
}
