package de.zannagh.armorhider.rendering;

import de.zannagh.armorhider.scopes.ItemRenderScope;
import de.zannagh.armorhider.scopes.ScopeProvider;
import de.zannagh.armorhider.util.ItemsUtil;
import de.zannagh.armorhider.util.ScopeUtils;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

//?if >= 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
//? }
//? if >= 1.21.4 && < 1.21.11 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
*///?}
//? if < 1.21.4 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * Applies visual modifications (transparency, render type swaps, color changes)
 * based on the current scope state.
 *
 * All methods are "pass-through safe": if no active scope or no modification
 * is needed, they return the original values unchanged.
 */
public final class RenderModifications {

    public static final int ELYTRA_RENDER_PRIORITY = 100;
    public static final int SKULL_RENDER_PRIORITY = 99;

    private RenderModifications() {}

    // --- Render type modifications ---

    //? if >= 1.21.11
    public static RenderType getSkullRenderLayer(@NotNull ScopeProvider scopes, Identifier texture, RenderType originalLayer) {
    //? if >= 1.21.4 && < 1.21.11
    //public static RenderType getSkullRenderLayer(@NotNull ScopeProvider scopes, ResourceLocation texture, RenderType originalLayer) {
    //? if < 1.21.4
    //public static RenderType getSkullRenderLayer(@NotNull ScopeProvider scopes, ResourceLocation texture, RenderType originalLayer) {
        var itemScope = ScopeUtils.getItemScopeIfModifiable(scopes);
        if (itemScope == null) {
            return originalLayer;
        }
        if (!itemScope.modification().playerConfig().opacityAffectingHatOrSkull.getValue()) {
            return originalLayer;
        }
        double transparency = itemScope.transparency();
        if (transparency < 1.0 && transparency >= 0) {
            return RenderTypeResolver.translucentEntity(texture);
        }
        return originalLayer;
    }

    //? if >= 1.21.11
    public static RenderType getTranslucentArmorRenderType(@NotNull ScopeProvider scopes, Identifier texture, RenderType originalLayer) {
    //? if >= 1.21.4 && < 1.21.11
    //public static RenderType getTranslucentArmorRenderType(@NotNull ScopeProvider scopes, ResourceLocation texture, RenderType originalLayer) {
    //? if < 1.21.4
    //public static RenderType getTranslucentArmorRenderType(@NotNull ScopeProvider scopes, ResourceLocation texture, RenderType originalLayer) {
        var itemScope = ScopeUtils.getItemScopeIfModifiable(scopes);
        if (itemScope == null) {
            return originalLayer;
        }
        double transparency = itemScope.transparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        return RenderTypeResolver.translucentArmor(texture);
    }

    //? if >= 1.21.4 {
    public static RenderType getTrimRenderLayer(@NotNull ScopeProvider scopes, boolean decal, RenderType originalLayer) {
        var itemScope = ScopeUtils.getItemScopeIfModifiable(scopes);
        if (itemScope == null) {
            return originalLayer;
        }
        double transparency = itemScope.transparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        return RenderTypeResolver.translucentArmorTrim();
    }
    //?}

    //? if < 1.21.4 {
    /*public static RenderType getTrimRenderLayer(@NotNull ScopeProvider scopes, boolean decal, RenderType originalLayer) {
        var itemScope = ScopeUtils.getItemScopeIfModifiable(scopes);
        if (itemScope == null) {
            return originalLayer;
        }
        double transparency = itemScope.transparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        return RenderTypeResolver.translucentArmorTrim();
    }
    *///?}

    public static RenderType getTranslucentItemRenderType(@NotNull ScopeProvider scopes, RenderType originalLayer) {
        var itemScope = ScopeUtils.getItemScopeIfModifiable(scopes);
        if (itemScope == null) {
            return originalLayer;
        }
        double transparency = itemScope.transparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        if (originalLayer == Sheets.cutoutBlockSheet()) {
            return RenderTypeResolver.translucentItemSheet();
        }
        return originalLayer;
    }

    // --- Color modifications ---

    public static int applyArmorTransparency(@NotNull ScopeProvider scopes, int originalColor) {
        var itemScope = ScopeUtils.getItemScopeIfModifiable(scopes);
        if (itemScope == null) {
            return originalColor;
        }
        int alpha = (int) (itemScope.transparency() * 255);
        return ColorMath.withAlpha(originalColor, alpha);
    }

    public static int applyTransparencyFromWhite(@NotNull ScopeProvider scopes, int original) {
        var itemScope = ScopeUtils.getItemScopeIfModifiable(scopes);
        if (itemScope == null) {
            return original;
        }
        int alpha = (int) (itemScope.transparency() * 255);
        return ColorMath.whiteWithAlpha(alpha);
    }

    public static float getTransparencyAlpha(@NotNull ScopeProvider scopes) {
        var itemScope = ScopeUtils.getItemScopeIfModifiable(scopes);
        if (itemScope == null) {
            return 1.0f;
        }
        return (float) itemScope.transparency();
    }

    // --- Priority modifications ---

    public static int modifyRenderPriority(@NotNull ScopeProvider scopes, int originalPriority) {
        var itemScope = scopes.itemScope();
        if (itemScope == null) {
            return originalPriority;
        }
        var stack = itemScope.itemStack();
        if (stack.is(Items.ELYTRA)) {
            return ELYTRA_RENDER_PRIORITY;
        }
        if (ItemsUtil.isSkullBlockItem(stack.getItem())) {
            return SKULL_RENDER_PRIORITY;
        }
        return originalPriority;
    }
}
