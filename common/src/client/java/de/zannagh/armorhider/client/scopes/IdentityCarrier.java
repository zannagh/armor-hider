package de.zannagh.armorhider.client.scopes;

import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.configuration.PlayerModificationInfo;
import de.zannagh.armorhider.client.api.configuration.SlotModification;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Duck interface injected onto {@code Player} (all versions) and
 * {@code LivingEntityRenderState} (>= 1.21.4, via {@link IdentityStateCarrier}).
 * <p>
 * Carries player identity and produces {@link ActiveModification} instances
 * for the rendering pipeline. Layer mixins call {@link #createModificationAndSetContext}
 * to get the pre-computed modification, then store it in {@code RenderContext}
 * for downstream render interceptors.
 */
public interface IdentityCarrier {
    @Nullable String armorHider$playerName();

    @Nullable ItemStack customHeadItem();

    boolean isPlayerFlying();

    PlayerModificationInfo armorHider$getPlayerModifications();

    /**
     * Creates a rendering modification for the given equipment slot and item without setting the render context.
     * Returns {@code null} when no modification is needed.
     */
    default @Nullable ActiveModification getModification(@NotNull EquipmentSlot slot, @Nullable ItemStack item) {
        return ActiveModification.create(armorHider$playerName(), slot, item);
    }

    /**
     * Creates a rendering modification for the given equipment slot and item.
     * Returns {@code null} when no modification is needed.
     * Also sets the active context if the modification is not null via {@link de.zannagh.armorhider.client.api.render.ArmorHiderRenderingScopeApi#setActiveModification(SlotModification)}
     */
    default SlotModification createModificationAndSetContext(@NotNull EquipmentSlot slot, @Nullable ItemStack item) {
        SlotModification cached = switch (slot) {
            case HEAD -> armorHider$getPlayerModifications().head();
            case CHEST -> armorHider$getPlayerModifications().chest();
            case LEGS -> armorHider$getPlayerModifications().legs();
            case FEET -> armorHider$getPlayerModifications().feet();
            default -> null;
        };
        SlotModification modification;
        if (cached != null && cached.item() != null && item != null && ItemStack.matches(cached.item(), item)) {
            modification = cached;
        } else {
            modification = SlotModification.of(armorHider$playerName(), slot, item);
        }
        if (!modification.isEmpty()) {
            ArmorHiderClientApi.getInstance().getRenderingScopeApi().setActiveModification(modification);
        } else {
            ArmorHiderClientApi.getInstance().getRenderingScopeApi().clearActiveModification();
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
