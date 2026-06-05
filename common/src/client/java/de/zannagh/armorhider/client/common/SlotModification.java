package de.zannagh.armorhider.client.common;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.configuration.items.ArmorOpacity;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the modifications that should be applied for a specific slot.
 * @param slot
 * @param shouldHide
 * @param shouldDisableGlint
 * @param transparency
 * @since 0.12.0
 */
public record SlotModification(
        EquipmentSlot slot,
        boolean needsModification,
        boolean shouldHide,
        boolean shouldDisableGlint,
        double transparency,
        String playerName,
        PlayerConfig config,
        ItemInfo itemInfo
) {

    /**
     * Returns the slot this modification applies to.
     * @return the slot this modification applies to, e.g. {@link EquipmentSlot#MAINHAND} or {@link EquipmentSlot#OFFHAND}
     */
    public EquipmentSlot slot() { return slot; }

    public static boolean shouldUseVanilla(PlayerConfig config){
        if (config.disableArmorHider.getValue()
            || (config.disableArmorHiderForOthers.getValue() && !config.playerName.getValue().equals(ArmorHiderClient.getCurrentPlayerName()))
            || config.playerName.getValue().isBlank()) {
            return true;
        }

        return false;
    }

    /**
     * Creates an empty slot modification.
     * @return An empty slot modification.
     */
    public static SlotModification empty(){
        return new SlotModification(EquipmentSlot.MAINHAND, false, false, false, 1.0, "", PlayerConfig.empty(), ItemInfo.empty());
    }

    public static SlotModification empty(EquipmentSlot slot){
        return new SlotModification(slot, false, false, false, 1.0, "", PlayerConfig.empty(), ItemInfo.empty());
    }

    public boolean isEmpty(){
        return playerName.isBlank();
    }

    public static boolean isEmpty(SlotModification modification) {
        return modification.playerName.isBlank();
    }

    public static SlotModification of(String playerName, EquipmentSlot slot, ItemStack itemStack) {
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);
        return of(config, slot).addItemInformation(new ItemInfo(itemStack));
    }

    public static SlotModification of(PlayerConfig config, EquipmentSlot slot) {

        if (shouldUseVanilla(config)) {
            return empty();
        }

        var transparency = switch (slot) {
            case HEAD -> config.helmetOpacity.getValue();
            case CHEST -> config.chestOpacity.getValue();
            case LEGS -> config.legsOpacity.getValue();
            case FEET -> config.bootsOpacity.getValue();
            case OFFHAND -> config.offHandOpacity.getValue();
            default -> 1.0;
        };

        boolean disableGlint = switch (slot) {
            case HEAD -> !config.helmetGlint.getValue();
            case CHEST -> !config.chestGlint.getValue();
            case LEGS -> !config.legsGlint.getValue();
            case FEET -> !config.bootsGlint.getValue();
            default -> false;
        };


        boolean shouldHideEntirely = transparency < ArmorOpacity.TRANSPARENCY_STEP;

        boolean needsModification = (transparency < 1 - ArmorOpacity.TRANSPARENCY_STEP / 2) || disableGlint;
        return new SlotModification(slot, needsModification, shouldHideEntirely, disableGlint, transparency, config.playerName.getValue(), config, null);
    }

    public SlotModification addItemInformation(ItemInfo itemInfo) {
        return addItemInformation(itemInfo.getStack());
    }

    /**
     * Adds additional item information to the slot modification in order to respect for example affecting skulls or elytra.
     * @param item The item to add information for
     * @return The modified slot modification
     */
    public SlotModification addItemInformation(@Nullable ItemStack item) {
        ItemStack resolvedItem = item != null ? item : ItemStack.EMPTY;
        ItemInfo resolvedItemInfo = new ItemInfo(resolvedItem);
        if (!resolvedItemInfo.isEmpty()) {
            var exclusionConfig = config.getExclusionItems();
            exclusionConfig.discoverItem(slot, resolvedItem.getItem(), resolvedItem.getHoverName().getString());

            if (exclusionConfig.shouldArmorHiderIgnore(slot, resolvedItem.getItem())) {
                return empty(slot);
            }
        }
        if (slot == EquipmentSlot.HEAD
                && ItemsUtil.isSkullBlockItem(resolvedItem.getItem())
                && !config.opacityAffectingHatOrSkull.getValue()) {
            return empty(slot);
        }

        if (slot == EquipmentSlot.CHEST
                && ItemsUtil.itemStackContainsElytra(resolvedItem)
                && !config.opacityAffectingElytra.getValue()) {
            return empty(slot);
        }

        return new SlotModification(slot, needsModification, shouldHide, shouldDisableGlint, transparency, playerName, config, resolvedItemInfo);
    }

    public static boolean isSlotModified(@NotNull String playerName, @NotNull EquipmentSlot slot, @NotNull ItemStack item) {
        var mod = of(playerName, slot, item);
        return mod.needsModification();
    }
}
