package de.zannagh.armorhider.client;

import net.fabricmc.api.ClientModInitializer;

public class FabricArmorHiderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ArmorHiderClient.init();
    }
}
