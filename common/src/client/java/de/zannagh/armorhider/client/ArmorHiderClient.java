package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.compat.elytratrims.CompatLoader;
import de.zannagh.armorhider.client.compat.elytratrims.ElytraTrimsCompat;
import de.zannagh.armorhider.client.net.ClientCommunicationManager;
import de.zannagh.armorhider.client.scopes.RenderContext;
import de.zannagh.armorhider.log.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import oshi.util.tuples.Pair;

public class ArmorHiderClient {

    public static int permissionLevel = 0; // Default to lowest.
    public static ClientConfigManager CLIENT_CONFIG_MANAGER = new ClientConfigManager();
    public static RenderContext RENDER_CONTEXT = new RenderContext();

    public static final boolean FA_LOADED = classExists("net.kenddie.fantasyarmor.FantasyArmor");

    /** True if GeckoLib is on the classpath — used to counteract mods like FantasyArmor
     *  that hide vanilla player-model arms when GeckoLib armor is equipped. */
    public static final boolean GECKOLIB_LOADED = classExists("software.bernie.geckolib.renderer.GeoArmorRenderer");

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
        initCompat();
    }

    private static void initCompat() {
        loadCompat("dev.kikugie.elytratrims.api.render.ETRenderingAPI",
                new ElytraTrimsCompat());
    }

    /**
     * Loads an optional compatibility module via reflection.
     * The compat class must have a public static {@code init()} method.
     *
     * @param apiClass    a class from the target mod; used as a presence check
     * @param compatClass the fully-qualified Armor Hider compat class to initialise
     */
    private static void loadCompat(String apiClass, CompatLoader compatClass) {
        try {
            if (!classExists(apiClass)) {
                return;
            }
            Class.forName(apiClass, false, ArmorHiderClient.class.getClassLoader());
            compatClass.init();
        } catch (ClassNotFoundException ignored) {
            // Target mod or compat class not present
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to initialise compat: {}", compatClass, e);
        }
    }
    
    public static @NonNull Boolean isClientConnectedToServer() {
        return Minecraft.getInstance().isLocalServer()
                || Minecraft.getInstance().getCurrentServer() != null
                || (Minecraft.getInstance().getConnection() != null && Minecraft.getInstance().getConnection().getServerData() != null);
    }

    public static String getCurrentPlayerName() {
        Player clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer == null) {
            return ClientConfigManager.DEFAULT_PLAYER_NAME;
        }
        Component displayText = clientPlayer.getDisplayName();
        return displayText.getString().isEmpty() ? ClientConfigManager.DEFAULT_PLAYER_NAME : displayText.getString();
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
        //? if >= 1.21.9
        String profileName = entry.getProfile().name();
        //? if < 1.21.9
        //String profileName = entry.getProfile().getName();
        if (profileName == null) {
            return new Pair<>(false, null);
        }
        return new Pair<>(!profileName.equals(getCurrentPlayerName()), entry);
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
        DebugLogger.log("--- Mod configuration ---");
        DebugLogger.log("The current local configuration is " + ArmorHiderClient.CLIENT_CONFIG_MANAGER.local().toJson());
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig() != null) {
            DebugLogger.log("The current server configuration is " + ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig().toJson());
        }
        else {
            DebugLogger.log("The current server configuration is null");
        }
        
        DebugLogger.log("--- End of mod configuration ---");
    }

    private static void disableDebugLogging() {
        DebugLogger.log("Stopped debug logging due to client request.");
        DebugLogger.disable();
    }
}