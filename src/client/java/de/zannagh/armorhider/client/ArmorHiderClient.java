package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.netPackets.SettingsC2SPacket;
import de.zannagh.armorhider.netPackets.SettingsS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ArmorHiderClient implements ClientModInitializer {

    public static Boolean IsCurrentPlayerSinglePlayerHostOrAdmin = false;
    
    @Override
	public void onInitializeClient() {
        ArmorHider.LOGGER.info("Armor Hider client initializing...");
        ClientPlayNetworking.registerGlobalReceiver(SettingsS2CPacket.IDENTIFIER, (payload, context) -> {
            ArmorHider.LOGGER.info("Armor Hider received configuration from server.");

            var serverConfigurationMap = payload.config();

            if (serverConfigurationMap == null) {
                ArmorHider.LOGGER.error("Failed to load settings packet.");
                return;
            }
            
            if (serverConfigurationMap.stream().anyMatch(c -> !c.enableCombatDetection)) {
                serverConfigurationMap.forEach(c -> c.enableCombatDetection = false);
                ClientConfigManager.get().enableCombatDetection = false;
            }
            else if (serverConfigurationMap.stream().allMatch(c -> c.enableCombatDetection)) {
                ClientConfigManager.get().enableCombatDetection = true;
            }

            ClientConfigManager.setServerConfig(serverConfigurationMap);
            ArmorHider.LOGGER.info("Armor Hider successfully set configuration from server.");
        });
        
        ClientConfigManager.load();
        
        ClientPlayConnectionEvents.JOIN.register((handler, packetSender, client) -> {
            assert client.player != null;
            var playerName = client.player.getName().getString();
            ClientConfigManager.updateName(playerName);
            ClientConfigManager.updateId(handler.getProfile().id());
            var currentConfig = ClientConfigManager.get();

            if (client.getServer() != null) {
                try {
                    IsCurrentPlayerSinglePlayerHostOrAdmin = client.getServer().getPermissionLevel(client.player.getPlayerConfigEntry()) >= 3;
                }
                catch (Exception ignored) {
                    ArmorHider.LOGGER.error("Failed to set permissions for player {}.", playerName);
                }
            }
            
            if (client.isInSingleplayer()) {
                IsCurrentPlayerSinglePlayerHostOrAdmin = true;
            }
            
            if (!IsCurrentPlayerSinglePlayerHostOrAdmin) {
                currentConfig.enableCombatDetection = true;
            }
            
            ClientPlayNetworking.send(new SettingsC2SPacket(currentConfig));
        });
    }
}