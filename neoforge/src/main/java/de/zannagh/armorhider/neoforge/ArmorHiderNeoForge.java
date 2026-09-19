package de.zannagh.armorhider.neoforge;

import de.zannagh.armorhider.ArmorHider;
import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.common.Mod;

@Mod("armor_hider")
public class ArmorHiderNeoForge {
    public ArmorHiderNeoForge(IEventBus modBus) {
        ArmorHider.init();
        // Payload codec/channel registration and dispatch are provided by the eunomia mod at runtime.
    }
}
