package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.networking.ClientCommunicationManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class ArmorHiderClient implements ClientModInitializer {

    public static Boolean isCurrentPlayerSinglePlayerHostOrAdmin = false;
    
    public static Boolean isClientConnectedToServer() {
        return MinecraftClient.getInstance().isConnectedToLocalServer()
                || MinecraftClient.getInstance().getServer() != null
                || (MinecraftClient.getInstance().getNetworkHandler() != null && MinecraftClient.getInstance().getNetworkHandler().getServerInfo() != null);
    }
    
    public static String getCurrentPlayerName() {
        // Java 17 compatibility: extract to variables instead of instanceof pattern matching
        ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
        if (clientPlayer != null) {
            net.minecraft.text.Text displayText = clientPlayer.getDisplayName();
            if (displayText != null) {
                return displayText.getString();
            }
        }
        return null;
    }
    
    public static ClientConfigManager CLIENT_CONFIG_MANAGER;
    
    @Override
	public void onInitializeClient() {
        ArmorHider.LOGGER.info("Armor Hider client initializing...");
        ClientCommunicationManager.initClient();
        CLIENT_CONFIG_MANAGER = new ClientConfigManager();
    }
}