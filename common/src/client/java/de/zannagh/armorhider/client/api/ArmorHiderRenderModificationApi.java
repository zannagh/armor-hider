package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.client.common.SlotModification;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Version-independent interface for applying render modifications.
 * <p>
 * Returned by {@link ScopeContext#renderModificationApi()} and available from
 * the scope API via {@link ArmorHiderRenderApi#getActiveScope(RenderScope)}.
 * All methods are pass-through safe: if no modification is active, original values are returned unchanged.
 * <p>
 * Render type methods use {@code Object} to avoid game-version dependencies in the API.
 * Callers should cast via {@code instanceof RenderType}.
 *
 * @since 0.12.0
 */
public interface ArmorHiderRenderModificationApi {

    int applyArmorTransparency(int originalColor);

    int applyTransparencyFromWhite(int original);

    float getTransparencyAlpha();

    boolean getHasFoil(boolean original);

    int modifyRenderPriority(int value);

    Object getTranslucentArmorRenderType(Object textureIdentifier, Object originalRenderType);

    Object getTranslucentRenderType(Object textureIdentifier, Object originalRenderType);

    Object getTrimRenderLayer(boolean decal, Object originalRenderType);

    Object getTranslucentItemRenderType(Object originalRenderType);

    Object getSkullRenderLayer(Object textureIdentifier, Object originalRenderType);

    default boolean isSlotFullyHiddenForPlayer(@NotNull String playerName, @NotNull EquipmentSlot slot, @NotNull ItemStack item) {
        var mod = SlotModification.of(playerName, slot, item);
        return mod.shouldHide();
    }
}
