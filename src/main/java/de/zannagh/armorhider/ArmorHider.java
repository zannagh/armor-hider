package de.zannagh.armorhider;

import de.zannagh.armorhider.common.EnrichedLogger;
import de.zannagh.armorhider.net.CommsManager;
import de.zannagh.armorhider.net.ServerRuntime;
import de.zannagh.armorhider.netPackets.SettingsC2SPacket;
import de.zannagh.armorhider.netPackets.SettingsS2CPacket;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.LoggerFactory;

public class ArmorHider implements ModInitializer {
    
    public static final Boolean TRANSLUCENCY_AFFECTING_OUTLINE = true;
	public static final String MOD_ID = "armor-hider";
	public static final EnrichedLogger LOGGER = new EnrichedLogger(LoggerFactory.getLogger(MOD_ID));

	@Override
	public void onInitialize() {
		LOGGER.info("Armor Hider initializing...");
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ServerRuntime.init(server);
            LOGGER.info("Server config store opened");
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (ServerRuntime.store != null) {
                ServerRuntime.store.save();
            }
        });
        PayloadTypeRegistry.playC2S().register(SettingsC2SPacket.IDENTIFIER, SettingsC2SPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(SettingsS2CPacket.IDENTIFIER, SettingsS2CPacket.PACKET_CODEC);
        CommsManager.initServer();
        LOGGER.info("Armor Hider initialized.");
	}
}