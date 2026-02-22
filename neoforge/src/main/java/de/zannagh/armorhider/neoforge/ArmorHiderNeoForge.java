package de.zannagh.armorhider.neoforge;

import de.zannagh.armorhider.ArmorHider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("armor-hider")
public class ArmorHiderNeoForge {
    public ArmorHiderNeoForge(IEventBus modBus) {
        ArmorHider.init();
    }
}
