package de.zannagh.armorhider;

import de.zannagh.armorhider.net.SettingsS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ArmorhiderClient implements ClientModInitializer {
    
    @Override
	public void onInitializeClient() {
        Armorhider.LOGGER.info("Armor Hider client initializing...");
        ClientPlayNetworking.registerGlobalReceiver(SettingsS2CPacket.IDENTIFIER, (payload, context) -> {
            Armorhider.LOGGER.info("Armor Hider received configuration from server.");
            
            var config = payload.config();
            
            if (config == null) {
                Armorhider.LOGGER.error("Failed to load settings packet.");
            }
            
            context.client().execute(() -> ClientConfigManager.setServerConfig(config));
        });
        ClientConfigManager.load();
	}
    
}