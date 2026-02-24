package de.zannagh.armorhider.quilt;

import de.zannagh.armorhider.ArmorHider;
import net.fabricmc.api.ModInitializer;

public class ArmorHiderQuilt implements ModInitializer {
    @Override
    public void onInitialize() {
        ArmorHider.init();
    }
}
