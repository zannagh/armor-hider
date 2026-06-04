package de.zannagh.armorhider.client.api.configuration;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record PlayerModificationInfo(
        SlotModification head,
        SlotModification chest,
        SlotModification legs,
        SlotModification feet,
        @Nullable ItemStack customHeadItem
) {
}
