package de.zannagh.armorhider.rendering;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import de.zannagh.armorhider.scopes.ScopeProvider;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

/**
 * Centralized render decision logic.
 * Queries the ScopeProvider to determine what should happen during rendering.
 */
public final class RenderDecisions {

    private RenderDecisions() {}

    /**
     * Whether armor rendering should be cancelled entirely (fully hidden at 0% opacity).
     * Requires both an active item scope and a resolved player entity.
     */
    public static boolean shouldCancelRender(@NotNull ScopeProvider scopes) {
        var itemScope = scopes.itemScope();
        if (itemScope == null) return false;

        var entityScope = scopes.entityScope();
        if (entityScope == null || !entityScope.isPlayerEntity()) return false;

        return shouldModifyEquipment(scopes) && itemScope.shouldHide();
    }

    /**
     * Whether equipment should be visually modified at all.
     * Checks global disable, server-wide force-off, per-player settings,
     * and skull/elytra special cases.
     */
    public static boolean shouldModifyEquipment(@NotNull ScopeProvider scopes) {
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.getValue()) {
            return false;
        }
        ServerConfiguration serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        if (serverConfig != null) {
            ServerWideSettings serverWideSettings = serverConfig.serverWideSettings;
            if (serverWideSettings != null && serverWideSettings.forceArmorHiderOff.getValue()) {
                return false;
            }
        }

        var itemScope = scopes.itemScope();
        if (itemScope == null) return false;

        ArmorModificationInfo modification = itemScope.modification();

        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHiderForOthers.getValue()
                && modification.isConfigForRemotePlayer(ArmorHiderClient.getCurrentPlayerName())) {
            return false;
        }

        if (modification.equipmentSlot() == EquipmentSlot.HEAD
                && ItemsUtil.isSkullBlockItem(itemScope.itemStack().getItem())
                && !modification.playerConfig().opacityAffectingHatOrSkull.getValue()) {
            return false;
        }

        if (modification.equipmentSlot() == EquipmentSlot.CHEST
                && ItemsUtil.itemStackContainsElytra(itemScope.itemStack())
                && !modification.playerConfig().opacityAffectingElytra.getValue()) {
            return false;
        }

        return modification.shouldModify();
    }

    /**
     * Whether equipment should be fully hidden (0% opacity).
     */
    public static boolean shouldHideEquipment(@NotNull ScopeProvider scopes) {
        var itemScope = scopes.itemScope();
        return itemScope != null && itemScope.shouldHide();
    }
}
