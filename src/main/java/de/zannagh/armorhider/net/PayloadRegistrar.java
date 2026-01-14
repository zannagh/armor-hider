package de.zannagh.armorhider.net;

import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PayloadRegistrar {
    public static void registerPayloads(){
        PayloadTypeRegistry.playC2S().register(PlayerConfig.TYPE, PlayerConfig.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ServerConfiguration.TYPE, ServerConfiguration.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ServerWideSettings.TYPE, ServerWideSettings.STREAM_CODEC);
    }
}
