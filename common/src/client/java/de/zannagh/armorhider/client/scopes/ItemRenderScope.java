package de.zannagh.armorhider.client.scopes;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import de.zannagh.armorhider.server.packets.PlayerConfig;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the scope of rendering a specific equipment item in a specific slot.
 * Active during renderArmorPiece / renderLayers / render for a specific layer renderer.
 * <p>
 * Holds the slot, item, and pre-resolved modification info.
 * Immutable value object — created on entry, read-only during scope, discarded on exit.
 */
public record ItemRenderScope(@NotNull EquipmentSlot slot,
                              @NotNull ItemStack itemStack,
                              @NotNull ArmorModificationInfo modification) {

    public boolean shouldHide() {
        return modification.shouldHide();
    }

    public boolean shouldModify() {
        return modification.shouldModify();
    }
    
    public boolean shouldDisableGlint() {
        return modification.shouldDisableGlint();
    }

    public double transparency() {
        return modification.getTransparency();
    }

    public static boolean isSlotFullyHidden(@NotNull String playerName, @NotNull EquipmentSlot slot, @NotNull ItemStack itemInSlot) {
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER == null) {
            return false;
        }
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderDisabled()) {
            return false;
        }
        
        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);
        if (config.disableArmorHiderForOthers.getValue()
                && !playerName.equals(ArmorHiderClient.getCurrentPlayerName())) {
            return false;
        }

        // Respect skull/elytra-specific settings
        if (slot == EquipmentSlot.HEAD && ItemsUtil.isSkullBlockItem(itemInSlot.getItem())
                && !config.opacityAffectingHatOrSkull.getValue()) {
            return false;
        }
        if (slot == EquipmentSlot.CHEST && ItemsUtil.itemStackContainsElytra(itemInSlot)
                && !config.opacityAffectingElytra.getValue()) {
            return false;
        }

        ArmorModificationInfo info = new ArmorModificationInfo(slot, config);
        return info.shouldHide();
    }
}
