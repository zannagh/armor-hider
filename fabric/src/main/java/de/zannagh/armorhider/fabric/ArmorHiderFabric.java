package de.zannagh.armorhider.fabric;

import de.zannagh.armorhider.ArmorHider;
import net.fabricmc.api.ModInitializer;

public class ArmorHiderFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ArmorHider.init();
    }
}
