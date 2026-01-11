package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.networking.ClientCommunicationManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import oshi.util.tuples.Pair;

public class ArmorHiderClient implements ClientModInitializer {

    public static Boolean isCurrentPlayerSinglePlayerHostOrAdmin = false;
    
    public static @NonNull Boolean isClientConnectedToServer() {
        return MinecraftClient.getInstance().isConnectedToLocalServer()
                || MinecraftClient.getInstance().getServer() != null
                || (MinecraftClient.getInstance().getNetworkHandler() != null && MinecraftClient.getInstance().getNetworkHandler().getServerInfo() != null);
    }
    
    public static String getCurrentPlayerName() { 
        return MinecraftClient.getInstance().player instanceof ClientPlayerEntity clientPlayer 
                && clientPlayer.getDisplayName() instanceof net.minecraft.text.Text displayText 
                ? displayText.getString() 
                : ClientConfigManager.DEFAULT_PLAYER_NAME;
    }
    
    @Contract("_ -> new")
    public static @NonNull Pair<Boolean, PlayerListEntry> isPlayerRemotePlayer(String playerName) {
        if (MinecraftClient.getInstance().getNetworkHandler() instanceof ClientPlayNetworkHandler networkHandler
                && networkHandler.getCaseInsensitivePlayerInfo(playerName) instanceof PlayerListEntry entry
                && entry.getDisplayName() != null) {
            return new Pair<>(!entry.getDisplayName().getString().equals(getCurrentPlayerName()), entry);
        }
        return new Pair<>(false, null);
    }
    
    public static ClientConfigManager CLIENT_CONFIG_MANAGER;
    
    @Override
	public void onInitializeClient() {
        ArmorHider.LOGGER.info("Armor Hider client initializing...");
        ClientCommunicationManager.initClient();
        CLIENT_CONFIG_MANAGER = new ClientConfigManager();
    }
}