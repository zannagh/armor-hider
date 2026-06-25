package de.zannagh.armorhider;

import net.fabricmc.api.ModInitializer;

public class ArmorHiderFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ArmorHider.init();
    }
}
