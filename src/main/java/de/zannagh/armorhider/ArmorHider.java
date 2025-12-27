package de.zannagh.armorhider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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
        CommsManager.initServer();
        LOGGER.info("Armor Hider initialized.");
	}
}