package de.zannagh.armorhider.client.compat.emf;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.util.PlayerNameUtil;
import traben.entity_model_features.EMFAnimationApi;

public final class EmfCompat {

    private static int callbackLogCounter = 0;

    private EmfCompat() {}

    public static void register() {
        try {
            EMFAnimationApi.registerVanillaModelCondition(emfEntity -> {
                var playerName = PlayerNameUtil.getPlayerName(emfEntity);
                if (playerName == null || ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderDisabled()) {
                    return false;
                }

                PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);
                if (!shouldApplyCombatDetection(config)) {
                    return false;
                }
                boolean inCombat = ArmorHiderApi.getInstance().getCombatManagement().isInCombat(playerName);
                boolean useDefault = config.inCombatUseDefaultModel.getValue();

                if (DebugLogger.isEnabled() && callbackLogCounter++ % 60 == 0) {
                    DebugLogger.log("[EMF callback] player={} | inCombat={} | useDefaultModel={} | entityClass={}",
                            playerName, inCombat, useDefault, emfEntity.getClass().getSimpleName());
                }

                if (!inCombat) {
                    return false;
                }
                return useDefault;
            });
            ArmorHider.LOGGER.debug("Registered vanilla model condition with EMF");
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register vanilla model condition with EMF", e);
        }
    }

    private static boolean shouldApplyCombatDetection(PlayerConfig config) {
        if (config.enableCombatDetection.getValue()) {
            return true;
        }
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        return serverConfig != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();
    }
}
