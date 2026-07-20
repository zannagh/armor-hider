package de.zannagh.armorhider.mixin;

import java.util.ArrayList;
import java.util.List;

public class NeoForgeMixinPlugin extends ArmorHiderMixinPlugin {
    private static final String[] GENERIC_MIXINS = new String[]{
            "MinecraftServerMixin",
            "ServerLoginMixin"
    };

    @Override
    public String getPackage() {
        return "de.zannagh.armorhider.net.mixin";
    }

    @Override
    public List<String> getExpectedMixins() {
        return new ArrayList<>(List.of(GENERIC_MIXINS));
    }
}
