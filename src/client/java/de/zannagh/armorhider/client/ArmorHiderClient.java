package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.netPackets.SettingsC2SPacket;
import de.zannagh.armorhider.netPackets.SettingsS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ArmorHiderClient implements ClientModInitializer {

    @Override
	public void onInitializeClient() {
        ArmorHider.LOGGER.info("Armor Hider client initializing...");
        ClientPlayNetworking.registerGlobalReceiver(SettingsS2CPacket.IDENTIFIER, (payload, context) -> {
            ArmorHider.LOGGER.info("Armor Hider received configuration from server.");

            var config = payload.config();

            if (config == null) {
                ArmorHider.LOGGER.error("Failed to load settings packet.");
                return;
            }

            ClientConfigManager.setServerConfig(config);
            ArmorHider.LOGGER.info("Armor Hider successfully set configuration from server.");
        });
        ClientConfigManager.load();
        ClientPlayConnectionEvents.JOIN.register((handler, packetSender, client) -> {
            assert client.player != null;
            var playerName = client.player.getName().getString();
            ClientConfigManager.updateName(playerName);
            ClientConfigManager.updateId(handler.getProfile().id());
            ClientPlayNetworking.send(new SettingsC2SPacket(ClientConfigManager.get()));
        });
    }
}