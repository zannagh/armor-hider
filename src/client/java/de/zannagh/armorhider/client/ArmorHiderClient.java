package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.networking.ClientCommunicationManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class ArmorHiderClient implements ClientModInitializer {

    public static Boolean IsCurrentPlayerSinglePlayerHostOrAdmin = false;
    
    public static Boolean IsClientConnectedToServer = 
            MinecraftClient.getInstance().isConnectedToLocalServer()
                || MinecraftClient.getInstance().getServer() != null
                || (MinecraftClient.getInstance().getNetworkHandler() != null && MinecraftClient.getInstance().getNetworkHandler().getServerInfo() != null);
    
    public static String CurrentPlayerName = MinecraftClient.getInstance().player instanceof ClientPlayerEntity clientPlayer && clientPlayer.getDisplayName() instanceof net.minecraft.text.Text displayText ? displayText.getString() : null;
    
    @Override
	public void onInitializeClient() {
        ArmorHider.LOGGER.info("Armor Hider client initializing...");
        ClientCommunicationManager.initClient();
        ClientConfigManager.load();
    }
}