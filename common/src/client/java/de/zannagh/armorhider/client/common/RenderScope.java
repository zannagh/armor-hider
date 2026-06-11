package de.zannagh.armorhider.client.common;

import de.zannagh.armorhider.common.ItemInfo;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Identifies the render scope that a modification applies to.
 * Each scope is independently managed — layer mixins enter/exit their scope,
 * and deep interceptors query the specific scope they care about.
 *
 * @since 0.12.0
 */
public enum RenderScope {
    NONE,
    ARMOR_PIECE,
    ELYTRA,
    CAPE,
    OFFHAND,
    HEAD,
    ALL;

    public static RenderScope of(EquipmentSlot slot, ItemInfo itemInfo) {
        return switch (slot) {
            case HEAD -> HEAD;
            case OFFHAND -> OFFHAND;
            case CHEST -> {
                if (itemInfo.isElytra()) {
                    yield ELYTRA;
                }
                yield ARMOR_PIECE;
            }
            default -> ARMOR_PIECE;
        };
    }
}
