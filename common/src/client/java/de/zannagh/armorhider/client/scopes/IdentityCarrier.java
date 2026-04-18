package de.zannagh.armorhider.client.scopes;

import de.zannagh.armorhider.ArmorHider;
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

    /**
     * Creates a rendering modification for the given equipment slot and item.
     * Returns {@code null} when no modification is needed.
     * Also sets the active context if the modification is not null via {@link RenderContext#setActiveModification(ActiveModification)} 
     */
    default @Nullable ActiveModification createModification(@NotNull EquipmentSlot slot, @Nullable ItemStack item) {
        
        var mod = ActiveModification.create(armorHider$playerName(), slot, item);
        if (mod != null) {
            ArmorHiderClient.RENDER_CONTEXT.setActiveModification(mod);
        } else {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
        }
        return mod;
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
