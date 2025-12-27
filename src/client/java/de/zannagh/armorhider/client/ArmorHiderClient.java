package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.networking.ClientCommunicationManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class ArmorHiderClient implements ClientModInitializer {

    public static Boolean IsCurrentPlayerSinglePlayerHostOrAdmin = false;
    
    public static Boolean IsClientConnectedToServer = 
            MinecraftClient.getInstance().isConnectedToLocalServer()
                || MinecraftClient.getInstance().getServer() != null
                || (MinecraftClient.getInstance().getNetworkHandler() != null && MinecraftClient.getInstance().getNetworkHandler().getServerInfo() != null);
    
    @Override
	public void onInitializeClient() {
        ArmorHider.LOGGER.info("Armor Hider client initializing...");
        ClientCommunicationManager.initClient();
        ClientConfigManager.load();
    }
}