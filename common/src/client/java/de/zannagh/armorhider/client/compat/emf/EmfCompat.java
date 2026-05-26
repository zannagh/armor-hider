//? if emf {
package de.zannagh.armorhider.client.compat.emf;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.combat.CombatManager;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.util.PlayerNameUtil;
import traben.entity_model_features.EMFAnimationApi;

public final class EmfCompat {

    private EmfCompat() {}

    public static void register() {
        try {
            EMFAnimationApi.registerVanillaModelCondition(emfEntity -> {
                var playerName = PlayerNameUtil.getPlayerName(emfEntity);
                if (playerName == null) return false;

                if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderDisabled()) return false;

                PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);
                if (!shouldApplyCombatDetection(config)) return false;
                if (!CombatManager.isInCombat(playerName)) return false;

                return config.inCombatUseDefaultModel.getValue();
            });
            ArmorHider.LOGGER.debug("Registered vanilla model condition with EMF");
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register vanilla model condition with EMF", e);
        }
    }

    private static boolean shouldApplyCombatDetection(PlayerConfig config) {
        if (config.enableCombatDetection.getValue()) return true;
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        return serverConfig != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();
    }
}
//?}
