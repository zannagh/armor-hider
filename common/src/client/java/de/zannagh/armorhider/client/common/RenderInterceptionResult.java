package de.zannagh.armorhider.client.common;

import de.zannagh.armorhider.common.ItemInfo;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

public record RenderInterceptionResult(
        boolean shouldIntercept,
        boolean shouldCancel,
        RenderScope scope,
        @Nullable IdentityCarrier carrier,
        @Nullable SlotModification modification
) {

    public static RenderInterceptionResult ignore() {
        return new RenderInterceptionResult(false, false, RenderScope.NONE, null, null);
    }

    public EquipmentSlot getSlot() {
        if (modification == null) {
            return null;
        }
        return modification.slot();
    }

    public ItemInfo getItemInfo() {
        if (modification == null) {
            return ItemInfo.empty();
        }
        return modification.itemInfo();
    }
}
