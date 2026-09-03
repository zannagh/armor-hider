//? if gender_physics && >= 1.21 {
package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.impl.AhRenderRuleRegistryImpl;
import de.zannagh.armorhider.client.api.impl.AhRuleTarget;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Decides whether Armor Hider should relax Wildfire's Female Gender Mod breast-physics damping for a
 * player, for {@code GenderPhysicsMixin} to feed into FGM's own "Armor Physics Override".
 * <p>
 * This deliberately does <em>not</em> reuse {@link SlotModification#shouldHide()}, for two reasons the
 * user issue turned on:
 * <ol>
 *   <li><b>Threshold.</b> {@code shouldHide()} is {@code opacity < 0.05} - literally the slider at 0%.
 *       A chestplate faded to 10-20% reads as gone to the eye but left the breasts fully damped. FGM's
 *       override is a boolean, so proportional damping is not on the table; {@link #RELAX_BELOW_OPACITY}
 *       is the point at which we call the plate "not really worn".</li>
 *   <li><b>Combat fade.</b> {@code shouldHide()} is derived <em>after</em>
 *       {@code CombatManager.transformTransparencyBasedOnCombat}, which temporarily raises opacity while
 *       combat detection is on. Keying physics off that made the jiggle switch itself off mid-fight and
 *       back on afterwards - the "works sometimes" the issue reports. Physics follow the user's
 *       <em>configured</em> intent ({@link SlotModification#configuredOpacityFor}) instead, so they stay
 *       stable across a fight. Rendering still follows the combat fade; only physics ignore it.</li>
 * </ol>
 * Registered API rules ARE honoured, so an API-only hide relaxes physics like a slider does.
 */
public final class GenderPhysicsRelaxation {

    /**
     * Chest opacity strictly below this relaxes the physics. Not a render threshold - the piece keeps
     * drawing at its configured opacity either way.
     */
    public static final double RELAX_BELOW_OPACITY = 0.5;

    private GenderPhysicsRelaxation() {}

    /**
     * @return {@code true} when the player's chest is hidden or faded far enough that FGM should stop
     *         damping the breast physics with it.
     */
    public static boolean shouldRelaxFor(Player player) {
        String playerName = PlayerNameUtil.getPlayerName(player);
        if (playerName == null || playerName.isBlank()) {
            return false;
        }
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(playerName);
        // Armor Hider is off for this player (globally, per-player, or "disable for others") - the plate
        // renders vanilla, so FGM must keep damping with it.
        if (SlotModification.shouldUseVanilla(config)) {
            return false;
        }

        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemInfo itemInfo = new ItemInfo(stack);
        // Deliberately only queries the exclusion list; unlike SlotModification#addItemInformation this
        // does NOT call discoverItem. This runs on both the client tick and the render thread, several
        // times per player per frame, and discoverItem mutates the config's discovered-item map.
        if (!itemInfo.isEmpty()
                && config.getExclusionItems().shouldArmorHiderIgnore(EquipmentSlot.CHEST, stack.getItem())) {
            return false;
        }

        // A plain elytra follows elytraOpacity and does not cover the breasts anyway; an armored elytra
        // stays on the chest slider, matching how SlotModification routes it.
        boolean isElytra = itemInfo.isElytra() && !itemInfo.isArmoredElytra();
        double opacity = SlotModification.configuredOpacityFor(config, EquipmentSlot.CHEST, isElytra);

        var ruled = AhRenderRuleRegistryImpl.evaluate(
                isElytra ? AhRuleTarget.ELYTRA : AhRuleTarget.of(EquipmentSlot.CHEST),
                playerName, EquipmentSlot.CHEST, stack, isElytra, config, opacity);
        if (ruled.changed()) {
            opacity = ruled.opacity();
        }

        return opacity < RELAX_BELOW_OPACITY;
    }
}
//?}
