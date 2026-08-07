package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatInitializationResult;
import de.zannagh.armorhider.api.compat.CompatInitializer;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhCombatApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.render.EmfHiddenModeContext;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.util.PlayerNameUtil;
import traben.entity_model_features.EMFAnimationApi;

//? if >= 1.21.4
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class EmfCompat implements CompatInitializer {

    private static int callbackLogCounter = 0;

    public EmfCompat() {}

    @Override
    public CompatFlags targetFlag() {
        return CompatFlags.ENTITY_MODEL_FEATURES;
    }

    @Override
    public CompatInitializationResult init() {
        try {
            EmfCompat.register();
            return CompatInitializationResult.SUCCESS;
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register vanilla model condition with EMF", e);
            return CompatInitializationResult.failure(e.getMessage());
        }
    }

    public static void register() {
        try {
            EMFAnimationApi.registerVanillaModelCondition(emfEntity -> {
                var playerName = PlayerNameUtil.getPlayerName(emfEntity);

                // #217 opt-in toggle: when the body (chest) region is hidden, honour the player's
                // "hidden model behaviour" setting. VANILLA forces the whole vanilla model here;
                // VANILLA_SEAMS lets EMF render and is handled per-part in EmfModelPartMixin (which
                // reads the mode we publish below); KEEP (default) leaves the custom model alone.
                de.zannagh.armorhider.configuration.EmfHiddenModelMode hiddenMode = hiddenModeFor(emfEntity);
                EmfHiddenModeContext.set(hiddenMode);
                if (hiddenMode == de.zannagh.armorhider.configuration.EmfHiddenModelMode.VANILLA) {
                    return true;
                }

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
     * The {@link de.zannagh.armorhider.configuration.EmfHiddenModelMode} that applies to the given
     * entity right now: the player's configured mode when the body (chest) region is hidden, else
     * {@link de.zannagh.armorhider.configuration.EmfHiddenModelMode#KEEP} (do nothing). Non-players and
     * players with a visible chest always resolve to {@code KEEP}.
     *
     * @param entity the entity EMF is about to render
     * @return the mode to apply
     */
    public static de.zannagh.armorhider.configuration.EmfHiddenModelMode hiddenModeFor(Object entity) {
        if (!bodyRegionHidden(entity) || !(entity instanceof IdentityCarrier carrier)) {
            return de.zannagh.armorhider.configuration.EmfHiddenModelMode.KEEP;
        }
        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(carrier.armorHider$playerName());
        return config.hiddenModelBehaviour.getValue();
    }

    /**
     * Whether the body (chest) region is currently hidden for the given entity, in which case EMF
     * should render the vanilla model to avoid exposing a custom player model's arm/torso seam
     * (#217). Resolved from the entity's live {@link de.zannagh.armorhider.client.common.PlayerModificationInfo}
     * (config + equipped items), so an empty chest slot or visible armor returns {@code false}.
     *
     * @param entity the entity EMF is about to render; only {@link IdentityCarrier} players qualify
     * @return {@code true} if the chest region resolves to hidden for this player
     */
    public static boolean bodyRegionHidden(Object entity) {
        if (!(entity instanceof IdentityCarrier carrier)) {
            return false;
        }
        var modifications = carrier.armorHider$getPlayerModifications();
        return modifications != null && modifications.chest().shouldHide();
    }

    /**
     * When EMF is loaded, skip clearing equipment from the render state.
     * Fresh Animations reads equipment state to determine arm/body poses;
     * clearing it causes arms to separate from the torso (#217).
     * Armor rendering is already prevented at the layer level by other mixins.
     * @param identityCarrier The identity carrier that is internally checked to be a {@link IdentityCarrier}
     * @param renderState The renderState that is internally checked to be a {@link AvatarRenderState}
     */
    public static void clearEquipment(Object identityCarrier, Object renderState) {
        // Skip clearing ONLY when EMF is present (Fresh Animations reads equipment off the render
        // state for arm/body poses - clearing it separates the arms, #217). For everyone else the
        // clear is the generic hide for modded/custom armor layers that read render-state equipment.
        if (CompatManager.requiresCompatTo(CompatFlags.ENTITY_MODEL_FEATURES)) {
            return;
        }
        if (!(identityCarrier instanceof IdentityCarrier carrier)) {
            return;
        }
        //? if >= 1.21.4 {
        if (!(renderState instanceof AvatarRenderState avRenderState)) {
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
        //? }
    }
}
