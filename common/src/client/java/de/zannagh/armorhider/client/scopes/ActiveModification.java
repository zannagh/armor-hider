package de.zannagh.armorhider.client.scopes;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.combat.CombatManager;
import de.zannagh.armorhider.configuration.items.ArmorOpacity;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pre-computed rendering modification for a single equipment item.
 * Created by layer mixins, consumed by deep render interceptors via {@link RenderContext}.
 * <p>
 * A non-null instance means the item SHOULD be modified — {@link #create} returns {@code null}
 * when no modification is needed (not a player, globally disabled, excluded item type, etc.).
 */
public record ActiveModification(
        @NotNull EquipmentSlot slot,
        @NotNull ItemStack item,
        @NotNull String playerName,
        double transparency,
        boolean shouldHide,
        boolean shouldDisableGlint
) {

    /**
     * Core factory. Returns {@code null} when no modification is needed:
     * <ul>
     *   <li>Player name is null (not a player entity)</li>
     *   <li>Armor Hider is globally disabled</li>
     *   <li>"Disable for others" is on and this is a remote player</li>
     *   <li>Skull/elytra is excluded by player config</li>
     *   <li>Transparency is near 1.0 and glint is not disabled</li>
     * </ul>
     */
    public static @Nullable ActiveModification create(@Nullable String playerName, @NotNull EquipmentSlot slot, @Nullable ItemStack item) {
        if (playerName == null) return null;
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderDisabled()) return null;

        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);

        if (config.disableArmorHiderForOthers.getValue()
                && !playerName.equals(ArmorHiderClient.getCurrentPlayerName())) {
            return null;
        }

        ItemStack resolvedItem = item != null ? item : ItemStack.EMPTY;
        EquipmentSlot resolvedSlot = ItemsUtil.itemStackContainsElytra(resolvedItem) ? EquipmentSlot.CHEST : slot;

        // Skull exclusion
        if (resolvedSlot == EquipmentSlot.HEAD
                && ItemsUtil.isSkullBlockItem(resolvedItem.getItem())
                && !config.opacityAffectingHatOrSkull.getValue()) {
            return null;
        }

        // Elytra exclusion
        if (resolvedSlot == EquipmentSlot.CHEST
                && ItemsUtil.itemStackContainsElytra(resolvedItem)
                && !config.opacityAffectingElytra.getValue()) {
            return null;
        }

        double transparency = getTransparencyForSlot(config, resolvedSlot);
        transparency = CombatManager.transformTransparencyBasedOnCombat(playerName, transparency);
        boolean disableGlint = getDisableGlintForSlot(config, resolvedSlot);

        boolean shouldModify = (transparency < 1 - ArmorOpacity.TRANSPARENCY_STEP / 2) || disableGlint;
        if (!shouldModify) return null;

        boolean shouldHide = transparency < ArmorOpacity.TRANSPARENCY_STEP;

        return new ActiveModification(resolvedSlot, resolvedItem, playerName, transparency, shouldHide, disableGlint);
    }

    /**
     * Checks if a slot is fully hidden for a player, without needing a render context.
     * Used by {@code EquipmentSlotHidingMixin} and {@code CapeRenderMixin}.
     */
    public static boolean isSlotFullyHidden(@NotNull String playerName, @NotNull EquipmentSlot slot, @NotNull ItemStack item) {
        var mod = create(playerName, slot, item);
        return mod != null && mod.shouldHide;
    }

    private static double getTransparencyForSlot(@NotNull PlayerConfig config, @NotNull EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> config.helmetOpacity.getValue();
            case CHEST -> config.chestOpacity.getValue();
            case LEGS -> config.legsOpacity.getValue();
            case FEET -> config.bootsOpacity.getValue();
            case OFFHAND -> config.offHandOpacity.getValue();
            default -> 1.0;
        };
    }

    private static boolean getDisableGlintForSlot(@NotNull PlayerConfig config, @NotNull EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> !config.helmetGlint.getValue();
            case CHEST -> !config.chestGlint.getValue();
            case LEGS -> !config.legsGlint.getValue();
            case FEET -> !config.bootsGlint.getValue();
            default -> false;
        };
    }
}
