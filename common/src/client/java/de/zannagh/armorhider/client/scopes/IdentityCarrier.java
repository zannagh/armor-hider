package de.zannagh.armorhider.client.scopes;

import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Duck interface injected onto {@code Player} (all versions) and
 * {@code LivingEntityRenderState} (>= 1.21.4, via {@link IdentityStateCarrier}).
 * <p>
 * Carries player identity and produces {@link ActiveModification} instances
 * for the rendering pipeline. Layer mixins call {@link #createModification}
 * to get the pre-computed modification, then store it in {@code RenderContext}
 * for downstream render interceptors.
 */
public interface IdentityCarrier {
    @Nullable String armorHider$playerName();

    @Nullable ItemStack customHeadItem();

    boolean isPlayerFlying();
    
    @Nullable ActiveModification armorHider$getHeadMod();
    @Nullable ActiveModification armorHider$getChestMod();
    @Nullable ActiveModification armorHider$getLegsMod();
    @Nullable ActiveModification armorHider$getFeetMod();

    /**
     * Creates a rendering modification for the given equipment slot and item.
     * Returns {@code null} when no modification is needed.
     * Also sets the active context if the modification is not null via {@link RenderContext#setActiveModification(ActiveModification)}
     */
    default @Nullable ActiveModification getModification(@NotNull EquipmentSlot slot, @Nullable ItemStack item) {

        return ActiveModification.create(armorHider$playerName(), slot, item);
    }

    /**
     * Creates a rendering modification for the given equipment slot and item.
     * Returns {@code null} when no modification is needed.
     * Also sets the active context if the modification is not null via {@link RenderContext#setActiveModification(ActiveModification)} 
     */
    default @Nullable ActiveModification createModification(@NotNull EquipmentSlot slot, @Nullable ItemStack item) {
        ActiveModification modification = null;
        if (slot == EquipmentSlot.HEAD && armorHider$getHeadMod() != null && item != null) {
            modification = armorHider$getHeadMod();
        }
        else if (slot == EquipmentSlot.CHEST && armorHider$getChestMod() != null && item != null) {
            modification = armorHider$getChestMod();
        }
        else if (slot == EquipmentSlot.LEGS && armorHider$getLegsMod() != null && item != null) {
            modification = armorHider$getLegsMod();
        }
        else if (slot == EquipmentSlot.FEET && armorHider$getFeetMod() != null && item != null) {
            modification = armorHider$getFeetMod();
        }
        else {
            modification = ActiveModification.create(armorHider$playerName(), slot, item);
        }
        if (modification != null) {
            ArmorHiderClient.RENDER_CONTEXT.setActiveModification(modification);
        } else {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
        }
        return modification;
    }

    /**
     * Signals that a compat layer (e.g. GeckoLib) needs the vanilla arm model
     * parts re-rendered because its custom armor model includes body geometry.
     */
    default void setNeedsArmRerender() {}

    /**
     * Returns and clears the arm re-render flag.
     */
    default boolean pollNeedsArmRerender() { return false; }

    /**
     * Saves the original GeckoLib render color before patching for transparency,
     * so it can be restored after GeckoLib finishes rendering the slot.
     */
    default void saveGeckoLibColor(int color) {}

    /**
     * Returns and clears the saved GeckoLib render color.
     */
    default @Nullable Integer pollSavedGeckoLibColor() { return null; }
}
