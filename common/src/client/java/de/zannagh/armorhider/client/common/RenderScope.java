package de.zannagh.armorhider.client.common;

import de.zannagh.armorhider.common.ItemInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Identifies one rendering "concern" that Armor Hider tracks independently. Each scope has its
 * own active modification context — layer mixins enter/exit their scope, and downstream
 * interceptors query the scope they care about.
 * <p>
 * Splitting these makes the interception pipeline composable: hiding the chest doesn't have to
 * coordinate with the cape mixin, and the head item can be transparent without dragging the
 * helmet slot along.
 *
 * @since 0.12.0
 */
public enum RenderScope {
    /** Sentinel used by {@link RenderInterceptionResult#ignore()}; no scope is active. */
    NONE,
    /** Vanilla armor pieces — head/chest/legs/feet armor models, both the equipment and humanoid layers. */
    ARMOR_PIECE,
    /** Elytra wings rendered as a chest-slot model — separate from {@link #ARMOR_PIECE} because the elytra has its own layer. */
    ELYTRA,
    /** Cape rendering — distinct from the chest slot so caped players keep their cape when chest armor is hidden. */
    CAPE,
    /** Off-hand item rendered in third person ({@code ItemInHandLayer}) and first person ({@code ItemInHandRenderer}). */
    OFFHAND,
    /** Worn head items (skulls, blocks-on-head) rendered by {@code CustomHeadLayer}. */
    HEAD,
    /** Fallback "any scope" key for the registry — see {@link de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi}. */
    ALL;

    /**
     * Map an equipment slot + item to the most specific scope it belongs to. Chest with an elytra
     * item resolves to {@link #ELYTRA}; everything else in chest/head/feet/legs is
     * {@link #ARMOR_PIECE}; offhand is {@link #OFFHAND}; head item is {@link #HEAD}.
     */
    public static RenderScope of(@Nullable EquipmentSlot slot, @Nullable ItemInfo itemInfo) {
        boolean isElytra = itemInfo != null && itemInfo.isElytra();
        if (slot == null) {
            return isElytra ? ELYTRA : ARMOR_PIECE;
        }
        return switch (slot) {
            case HEAD -> HEAD;
            case OFFHAND -> OFFHAND;
            case CHEST -> isElytra ? ELYTRA : ARMOR_PIECE;
            default -> ARMOR_PIECE;
        };
    }

    public static RenderScope of(@Nullable EquipmentSlot slot, @NonNull ItemStack stack) {
        return of(slot, new ItemInfo(stack));
    }
}
