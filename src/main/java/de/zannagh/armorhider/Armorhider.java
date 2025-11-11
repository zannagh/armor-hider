package de.zannagh.armorhider;

import de.zannagh.armorhider.net.NetworkManager;
import de.zannagh.armorhider.net.SettingsC2SPacket;
import de.zannagh.armorhider.net.SettingsS2CPacket;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Armorhider implements ModInitializer {
	public static final String MOD_ID = "armor-hider";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Armor Hider initializing...");
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            NetworkManager.ServerRuntime.init(server);
            LOGGER.info("Server config store opened");
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (NetworkManager.ServerRuntime.store != null) {
                NetworkManager.ServerRuntime.store.save();
            }
        });
        PayloadTypeRegistry.playC2S().register(SettingsC2SPacket.IDENTIFIER, SettingsC2SPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(SettingsS2CPacket.IDENTIFIER, SettingsS2CPacket.PACKET_CODEC);
        NetworkManager.initServer();
        LOGGER.info("Armor Hider initialized.");
	}
}