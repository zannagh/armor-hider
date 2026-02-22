package de.zannagh.armorhider.neoforge;

import de.zannagh.armorhider.client.ArmorHiderClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = "armor-hider", dist = Dist.CLIENT)
public class ArmorHiderNeoForgeClient {
    public ArmorHiderNeoForgeClient(IEventBus modBus) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ArmorHiderClient.init();
    }
}
