package de.zannagh.armorhider.neoforge;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.net.ClientConnectionEvents;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "armor_hider", dist = Dist.CLIENT)
public class ArmorHiderNeoForgeClient {
    public ArmorHiderNeoForgeClient(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onClientLogin);
        ArmorHiderClient.init();
    }


    private void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        var listener = Minecraft.getInstance().getConnection();
        if (listener != null) {
            ClientConnectionEvents.onClientJoin(listener, Minecraft.getInstance());
        }
    }
}
