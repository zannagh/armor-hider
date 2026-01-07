package de.zannagh.armorhider.net;

import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PayloadRegistrar {
    public static void registerPayloads(){
        var playerConfig = PlayerConfig.empty();
        PayloadTypeRegistry.playC2S().register(playerConfig.getId(), playerConfig.getCodec());

        var serverConfig = new ServerConfiguration();
        var serverWideSettings = new ServerWideSettings();
        PayloadTypeRegistry.playS2C().register(serverConfig.getId(), serverConfig.getCodec());
        PayloadTypeRegistry.playC2S().register(serverWideSettings.getId(), serverWideSettings.getCodec());
    }
}
