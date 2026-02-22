package de.zannagh.armorhider.neoforge;

import de.zannagh.armorhider.ArmorHider;
import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod("armor_hider")
public class ArmorHiderNeoForge {
    public ArmorHiderNeoForge(IEventBus modBus) {
        ArmorHider.init();
        modBus.addListener(this::onRegisterPayloadHandlers);
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        NeoForgePayloadHandler.registerAll(event.registrar("1"));
    }
}
