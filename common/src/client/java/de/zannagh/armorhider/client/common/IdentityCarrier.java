package de.zannagh.armorhider.client.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * Duck interface injected onto {@code Player} (all versions) and
 * {@code LivingEntityRenderState} (>= 1.21.4, via {@link IdentityStateCarrier}).
 * <p>
 * Carries player identity and produces {@link SlotModification} instances for the rendering pipeline.
 */
public interface IdentityCarrier {
    @Nullable String armorHider$playerName();

    @Nullable ItemStack ah$getCustomHeadItem();

    boolean ah$isPlayerFlying();

    default boolean ah$isPlayerBlocking() { return false; }

    default boolean armorHider$isPlayerInvisible() { return false; }

    PlayerModificationInfo armorHider$getPlayerModifications();

    default boolean armorHider$allSlotsFullyHidden() {
        var mods = armorHider$getPlayerModifications();
        if (mods == null) return false;
        return mods.head().shouldHide()
                && mods.chest().shouldHide()
                && mods.legs().shouldHide()
                && mods.feet().shouldHide();
    }

    // Uniquely namespaced: the name must NOT collide with the vanilla LivingEntity#getItemBySlot,
    // otherwise in a remapped (production) environment Player has no method matching this interface
    // signature and any interface-typed call throws AbstractMethodError.
    @NonNull ItemStack armorHider$getItemBySlot(EquipmentSlot slot);

    /**
     * Creates a rendering modification for the given equipment slot and item without setting the render context.
     * Returns {@code null} when no modification is needed.
     */
    default SlotModification ah$getModification(@NotNull EquipmentSlot slot, @Nullable ItemStack item) {
        var mods = armorHider$getPlayerModifications();
        if (mods == null) {
            return SlotModification.empty();
        }
        var slotInfo = switch (slot) {
            case HEAD -> mods.head();
            case CHEST -> mods.chest();
            case LEGS -> mods.legs();
            case FEET -> mods.feet();
            default -> SlotModification.empty();
        };
        if (item != null) {
            slotInfo = slotInfo.addItemInformation(item);
        }
        return slotInfo;
    }

}
