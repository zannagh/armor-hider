package de.zannagh.armorhider.client.suppressions;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhRenderer;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;

public class InvisibilitySuppressor implements ConditionalSuppressor {
    @Override
    public boolean shouldSuppress(RenderScope scope, IdentityCarrier carrier) {
        if (!carrier.armorHider$isPlayerInvisible()) {
            return false;
        }

        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        if (serverConfig != null
                && serverConfig.serverWideSettings.disableArmorHiderOnInvisibilityGlobally.getValue()) {
            return true;
        }

        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(carrier.armorHider$playerName());
        return config.disableArmorHiderOnInvisibility.getValue();
    }

    @Override
    public boolean shouldSuppress(RenderScope scope, AhRenderer renderer) {
        return false;
    }
}
