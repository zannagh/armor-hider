package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhCombatApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.util.PlayerNameUtil;
import traben.entity_model_features.EMFAnimationApi;

//? if >= 1.21.4
//import net.minecraft.client.renderer.entity.state.PlayerRenderState;

public final class EmfCompat {

    private static int callbackLogCounter = 0;

    private EmfCompat() {}

    public static void register() {
        try {
            EMFAnimationApi.registerVanillaModelCondition(emfEntity -> {
                var playerName = PlayerNameUtil.getPlayerName(emfEntity);
                boolean inCombat = ArmorHiderApi.getInstance().getCombatManagement().isInCombat(playerName);

                if (!inCombat || !AhCombatApi.shouldApplyCombatDetectionFor(playerName)) {
                    return false;
                }

                PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(playerName);

                boolean useDefault = config.inCombatUseDefaultModel.getValue();

                if (DebugLogger.isEnabled() && callbackLogCounter++ % 60 == 0) {
                    DebugLogger.log("[EMF callback] player={} | useDefaultModel={} | entityClass={}",
                            playerName, useDefault, emfEntity.getClass().getSimpleName());
                }

                return useDefault;
            });
            ArmorHider.LOGGER.debug("Registered vanilla model condition with EMF");
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register vanilla model condition with EMF", e);
        }
    }

    /**
     * When EMF is loaded, skip clearing equipment from the render state.
     * Fresh Animations reads equipment state to determine arm/body poses;
     * clearing it causes arms to separate from the torso (#217).
     * Armor rendering is already prevented at the layer level by other mixins.
     * @param identityCarrier The identity carrier that is internally checked to be a {@link IdentityCarrier}
     * @param renderState The renderState that is internally checked to be a {@link PlayerRenderState}
     */
    public static void clearEquipment(Object identityCarrier, Object renderState) {
        if (!ArmorHiderClient.EMF_LOADED) {
            return;
        }
        if (!(identityCarrier instanceof IdentityCarrier carrier)) {
            return;
        }
        //? if >= 1.21.4 {
        /*if (!(renderState instanceof PlayerRenderState avRenderState)) {
            return;
        }
        if (carrier.armorHider$getPlayerModifications().head().shouldHide()) {
            avRenderState.headEquipment.copyAndClear();
        }
        if (carrier.armorHider$getPlayerModifications().chest().shouldHide()) {
            avRenderState.chestEquipment.copyAndClear();
        }
        if (carrier.armorHider$getPlayerModifications().legs().shouldHide()) {
            avRenderState.legsEquipment.copyAndClear();
        }
        if (carrier.armorHider$getPlayerModifications().feet().shouldHide()) {
            avRenderState.feetEquipment.copyAndClear();
        }
        *///? }
    }
}
