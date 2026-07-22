package de.zannagh.armorhider.mixin;

import java.util.ArrayList;
import java.util.List;

public class FabricMixinPlugin extends ArmorHiderMixinPlugin {

    private static final String[] GENERIC_MIXINS = new String[]{
            "MinecraftServerMixin",
            "ServerLoginMixin",
    };

    private static final String[] AT_OR_ABOVE_1_20_5_MIXINS_FABRIC = new String[]{
            "ClientboundCustomPayloadPacketMixin",
            "ServerboundCustomPayloadPacketMixin",
            "ServerGamePacketListenerMixin"
    };

    private static final String[] BELOW_1_20_5_MIXINS = new String[]{
            "ServerPlayNetworkHandlerMixin"
    };


    @Override
    public String getPackage() {
        return "de.zannagh.armorhider.net.mixin";
    }

    @Override
    public List<String> getExpectedMixins() {
        var mixinsToAdd = new ArrayList<String>();
        mixinsToAdd.addAll(List.of(GENERIC_MIXINS));

        //? if >= 1.20.5
        mixinsToAdd.addAll(List.of(AT_OR_ABOVE_1_20_5_MIXINS_FABRIC));
        //? if < 1.20.5
        //mixinsToAdd.addAll(List.of(BELOW_1_20_5_MIXINS));

        //? if >= 26.1-0.snapshot.11
        //mixinsToAdd.add("PackRepositoryMixin");
        return mixinsToAdd;
    }
}
