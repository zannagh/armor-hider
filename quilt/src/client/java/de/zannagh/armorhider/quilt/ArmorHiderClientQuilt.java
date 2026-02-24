package de.zannagh.armorhider.quilt;

import de.zannagh.armorhider.client.ArmorHiderClient;
import net.fabricmc.api.ClientModInitializer;

public class ArmorHiderClientQuilt implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ArmorHiderClient.init();
    }
}
