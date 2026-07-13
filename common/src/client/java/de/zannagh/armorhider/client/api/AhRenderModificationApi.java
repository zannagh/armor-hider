package de.zannagh.armorhider.client.api;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.RenderScopeContext;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.render.RenderModifications;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Version-independent interface for applying render modifications (transparency, render type
 * swaps, glint toggling, color tweaks). Pass-through-safe: when no modification is active,
 * every method returns the original value unchanged.
 * <p>
 * Obtain an instance via {@link RenderScopeContext#renderModificationApi()} (preferred — the
 * context already carries the resolved modification) or via
 * {@link AhRenderManagementApi#getActiveScope(RenderScope)} for the active scope. For one-off
 * lookups with a known {@link SlotModification}, use {@link #getInstance(SlotModification)}.
 * <p>
 * <b>Render type methods use {@code Object}</b> rather than {@code RenderType} so this API stays
 * game-version independent. Callers cast via {@code instanceof RenderType} and fall back to the
 * original value when the cast fails.
 *
 * @since 0.12.0
 */
@ApiStatus.NonExtendable
public interface AhRenderModificationApi {

    ArrayList<Pair<Integer, AhRenderTypeFactory>> RENDER_TYPE_FACTORIES = new ArrayList<>();

    ArrayList<Pair<Integer, AhColorTransformer>> COLOR_TRANSFORMERS = new ArrayList<>();

    ArrayList<Pair<Integer, AhRenderModificationApi>> RENDER_MODIFIERS = new ArrayList<>();

    /**
     * Returns a modification API instance backed by the given {@link SlotModification}. Mostly
     * useful for compat code that resolves a {@code SlotModification} by hand and wants to apply
     * the same render transformations without entering a scope.
     */
    static AhRenderModificationApi getInstance(SlotModification modification) {
        AhRenderModificationApi instance;
        if (RENDER_MODIFIERS.isEmpty()) {
            instance = new RenderModifications(modification);
        }
        else {
            instance = RENDER_MODIFIERS.get(0).getSecond();
        }
        if (!RENDER_TYPE_FACTORIES.isEmpty()) {
            instance.setRenderTypeFactory(RENDER_TYPE_FACTORIES.get(0).getSecond());
        }
        if (!COLOR_TRANSFORMERS.isEmpty()) {
            instance.setColorTransformer(COLOR_TRANSFORMERS.get(0).getSecond());
        }
        return instance;
    }

    static int getDefaultPriority() {
        return 1000;
    }

    static void registerRenderTypeFactory(AhRenderTypeFactory renderTypeFactory, int priority) {
        ArmorHider.LOGGER.info("Registering render type factory: {} with priority {}", renderTypeFactory.getClass().getName(), priority);
        RENDER_TYPE_FACTORIES.add(Pair.of(priority, renderTypeFactory));
        RENDER_TYPE_FACTORIES.sort(Comparator.comparingInt(Pair::getFirst));
    }

    /**
     * Register a custom {@link AhColorTransformer}. Lower priority values win — mirrors
     * {@link #registerRenderTypeFactory(AhRenderTypeFactory, int)}. Useful for compat layers that
     * want gamma-corrected blending or shader-friendly color spaces, and for the mod itself to
     * conditionally swap arithmetic per compat scenario without touching mixin call sites.
     */
    static void registerColorTransformer(AhColorTransformer colorTransformer, int priority) {
        ArmorHider.LOGGER.info("Registering color transformer: {} with priority {}", colorTransformer.getClass().getName(), priority);
        COLOR_TRANSFORMERS.add(Pair.of(priority, colorTransformer));
        COLOR_TRANSFORMERS.sort(Comparator.comparingInt(Pair::getFirst));
    }

    void setRenderTypeFactory(AhRenderTypeFactory renderTypeFactory);

    AhRenderTypeFactory renderTypes();

    void setColorTransformer(AhColorTransformer colorTransformer);

    /**
     * Returns the {@link AhColorTransformer} used by this instance — either a registered override
     * (highest precedence) or the built-in {@code DefaultColorTransformer}. Use the returned
     * transformer for any color arithmetic that needs to honor compat overrides, rather than
     * doing the bit-twiddling inline at each call site.
     */
    AhColorTransformer colors();

    /**
     * Apply the active transparency to the alpha channel of an ARGB color.
     *
     * @param originalColor the source ARGB color.
     * @return the color with its alpha multiplied by the active transparency, or the original color
     * if no modification is active.
     */
    int applyArmorTransparency(int originalColor);

    /**
     * Produce a translucent white color (opaque white with its alpha scaled by the active
     * transparency). Used by render layers whose vanilla color is white (e.g. armor trims,
     * skulls). Returns opaque white ({@code 0xFFFFFFFF}) when no modification is active, so
     * callers never have to supply — or accidentally mis-supply — a fallback color.
     */
    int applyTransparencyFromWhite();

    /**
     * @return the active transparency as an alpha factor in {@code [0.0, 1.0]}, or {@code 1.0} when
     * no modification is active (i.e. fully opaque).
     */
    float getTransparencyAlpha();

    /**
     * Toggle the enchantment-glint flag according to the active modification's
     * {@code shouldDisableGlint} setting.
     *
     * @param original the upstream "has foil" decision.
     * @return {@code false} when the modification wants the glint disabled; otherwise the original
     * value.
     */
    boolean getHasFoil(boolean original);

    /**
     * Adjust render priority — used in 1.21.9+ to re-order the elytra and skull layers so they
     * draw after armor (avoids translucent-armor occluding them). Returns the original priority
     * when no modification is active.
     */
    int modifyRenderPriority(int value);

    /**
     * Swap an armor-cutout render type for a translucent one when a modification is active.
     *
     * @param textureResourceLocation the resolved armor texture ({@code ResourceLocation}).
     * @param originalRenderType the upstream {@code RenderType}.
     * @return a translucent {@code RenderType} (cast via {@code instanceof}), or the original when
     * no modification is active.
     */
    Object getTranslucentArmorRenderType(Object textureResourceLocation, Object originalRenderType);

    /**
     * Swap the armor-trim render type for a translucent equivalent.
     *
     * @param decal whether the trim is a decal (overlay) layer.
     */
    Object getTrimRenderLayer(boolean decal, Object originalRenderType);

    /**
     * Swap a block-item-sheet render type for a translucent one — used for offhand banners and
     * shields on MC versions where they don't use the entity sheet.
     */
    Object getTranslucentItemRenderType(Object originalRenderType);

    /**
     * Swap the skull (player-head / mob-head) render type for a translucent one.
     */
    Object getSkullRenderLayer(Object textureResourceLocation, Object originalRenderType);

    /**
     * Convenience helper: whether the given (player, slot, item) triple resolves to a slot the
     * player wants fully hidden. Equivalent to building a {@link SlotModification} and checking
     * {@link SlotModification#shouldHide()}.
     */
    default boolean isSlotFullyHiddenForPlayer(@NotNull String playerName, @NotNull EquipmentSlot slot, @NotNull ItemStack item) {
        var mod = SlotModification.of(playerName, slot, item);
        return mod.shouldHide();
    }
}
