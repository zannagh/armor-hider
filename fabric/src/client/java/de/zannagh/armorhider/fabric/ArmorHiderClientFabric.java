package de.zannagh.armorhider.fabric;

import de.zannagh.armorhider.client.ArmorHiderClient;
import net.fabricmc.api.ClientModInitializer;

public class ArmorHiderClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ArmorHiderClient.init();
    }
}
