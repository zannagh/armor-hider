package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.CompatFlags;
import de.zannagh.armorhider.client.api.ArmorHiderClientApiImpl;
import de.zannagh.armorhider.client.gui.screens.ArmorHiderOptionsScreen;
import de.zannagh.armorhider.client.net.ClientCommunicationManager;
import de.zannagh.armorhider.configuration.PresetManager;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import oshi.util.tuples.Pair;

//? if >= 1.21 {
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
//?} else {
/*import net.minecraft.client.gui.screens.SkinCustomizationScreen;
*///?}

public class ArmorHiderClient {

    public static int permissionLevel = 0; // Default to lowest.
    public static ClientConfigManager CLIENT_CONFIG_MANAGER = new ClientConfigManager();
    public static PresetManager PRESET_MANAGER = new PresetManager();

    public static final boolean FA_LOADED = CompatFlags.FA_LOADED || classExists("net.kenddie.fantasyarmor.FantasyArmor");
    public static final boolean GECKOLIB_LOADED = CompatFlags.GECKOLIB_LOADED || classExists("com.geckolib.renderer.GeoArmorRenderer");
    public static final boolean ET_LOADED = CompatFlags.ET_LOADED || classExists("dev.kikugie.elytratrims.ep.ETClientEntrypoint");
    public static final boolean IRIS_LOADED = classExists("net.irisshaders.iris.api.v0.IrisApi");

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
        ArmorHiderClientApiImpl.init();
        if (IRIS_LOADED) {
            initIrisCompat();
        }
        if (CompatFlags.EMF_LOADED) {
            initEmfCompat();
        }
    }

    private static void initEmfCompat() {
        try {
            de.zannagh.armorhider.client.compat.emf.EmfCompat.register();
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register vanilla model condition with EMF", e);
        }
    }

    private static void initIrisCompat() {
        try {
            de.zannagh.armorhider.client.compat.iris.IrisCompat.registerPipelines();
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register pipelines with Iris", e);
        }
    }
    
    public static @NonNull Boolean isClientConnectedToServer() {
        return Minecraft.getInstance().isLocalServer()
                || Minecraft.getInstance().getCurrentServer() != null
                || (Minecraft.getInstance().getConnection() != null && Minecraft.getInstance().getConnection().getServerData() != null);
    }

    public static void openPreferredSettingsScreen(Screen parent, net.minecraft.client.Options options) {
        var minecraft = Minecraft.getInstance();
        //? if >= 1.21.9 {
        Screen target = CLIENT_CONFIG_MANAGER.getValue().showSettingsInSkinCustomization.getValue()
                ? new SkinCustomizationScreen(parent, options)
                : new ArmorHiderOptionsScreen(parent, options);
        //?}
        //? if < 1.21.9
        //Screen target = new ArmorHiderOptionsScreen(parent, options);
        minecraft.setScreenAndShow(target);
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
