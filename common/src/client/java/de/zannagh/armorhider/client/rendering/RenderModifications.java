package de.zannagh.armorhider.client.rendering;

import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.RenderContext;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

//?if >= 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
//? }
//? if >= 1.21.4 && < 1.21.11 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
*///?}
//? if < 1.21.4 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
*///?}

/**
 * Applies visual modifications (transparency, render type swaps, color changes)
 * based on the active modification in the render context.
 *
 * All methods are "pass-through safe": if no active modification exists,
 * they return the original values unchanged.
 */
public final class RenderModifications {

    public static final int ELYTRA_RENDER_PRIORITY = 100;
    public static final int SKULL_RENDER_PRIORITY = 99;

    private RenderModifications() {}

    // --- Render type modifications ---

    //? if >= 1.21.11
    public static RenderType getSkullRenderLayer(@NotNull RenderContext ctx, Identifier texture, RenderType originalLayer) {
    //? if >= 1.21.4 && < 1.21.11
    //public static RenderType getSkullRenderLayer(@NotNull RenderContext ctx, Identifier texture, RenderType originalLayer) {
    //? if < 1.21.4
    //public static RenderType getSkullRenderLayer(@NotNull RenderContext ctx, Identifier texture, RenderType originalLayer) {
        var mod = ctx.activeModification();
        if (mod == null) return originalLayer;
        double transparency = mod.transparency();
        if (transparency < 1.0 && transparency >= 0) {
            return RenderTypeResolver.translucentEntity(texture);
        }
        return originalLayer;
    }

    //? if >= 1.21.11
    public static RenderType getTranslucentArmorRenderType(@NotNull RenderContext ctx, Identifier texture, RenderType originalLayer) {
    //? if >= 1.21.4 && < 1.21.11
    //public static RenderType getTranslucentArmorRenderType(@NotNull RenderContext ctx, Identifier texture, RenderType originalLayer) {
    //? if < 1.21.4
    //public static RenderType getTranslucentArmorRenderType(@NotNull RenderContext ctx, Identifier texture, RenderType originalLayer) {
        var mod = ctx.activeModification();
        if (mod == null) return originalLayer;
        double transparency = mod.transparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        return RenderTypeResolver.translucentArmor(texture);
    }

    //? if >= 1.21.4 {
    public static RenderType getTrimRenderLayer(@NotNull RenderContext ctx, boolean decal, RenderType originalLayer) {
        var mod = ctx.activeModification();
        if (mod == null) return originalLayer;
        double transparency = mod.transparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        return RenderTypeResolver.translucentArmorTrim();
    }
    //?}

    //? if < 1.21.4 {
    /*public static RenderType getTrimRenderLayer(@NotNull RenderContext ctx, boolean decal, RenderType originalLayer) {
        var mod = ctx.activeModification();
        if (mod == null) return originalLayer;
        double transparency = mod.transparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        return RenderTypeResolver.translucentArmorTrim();
    }
    *///?}

    public static RenderType getTranslucentItemRenderType(@NotNull RenderContext ctx, RenderType originalLayer) {
        var mod = ctx.activeModification();
        if (mod == null) return originalLayer;
        double transparency = mod.transparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        if (originalLayer == Sheets.cutoutBlockSheet()) {
            return RenderTypeResolver.translucentItemSheet();
        }
        return originalLayer;
    }

    // --- Color modifications ---

    public static int applyArmorTransparency(@NotNull RenderContext ctx, int originalColor) {
        var mod = ctx.activeModification();
        if (mod == null) return originalColor;
        int alpha = (int) (mod.transparency() * 255);
        return ColorMath.withAlpha(originalColor, alpha);
    }

    public static int applyTransparencyFromWhite(@NotNull RenderContext ctx, int original) {
        var mod = ctx.activeModification();
        if (mod == null) return original;
        int alpha = (int) (mod.transparency() * 255);
        return ColorMath.whiteWithAlpha(alpha);
    }

    public static float getTransparencyAlpha(@NotNull RenderContext ctx) {
        var mod = ctx.activeModification();
        if (mod == null) return 1.0f;
        return (float) mod.transparency();
    }

    // --- Priority modifications ---

    public static int modifyRenderPriority(@NotNull RenderContext ctx, int originalPriority) {
        var mod = ctx.activeModification();
        if (mod == null) return originalPriority;
        if (mod.item().is(Items.ELYTRA)) {
            return ELYTRA_RENDER_PRIORITY;
        }
        if (ItemsUtil.isSkullBlockItem(mod.item().getItem())) {
            return SKULL_RENDER_PRIORITY;
        }
        return originalPriority;
    }

    // Buffer wrapping for < 1.21.9 (used by ItemInHandLayerMixin and OffHandRenderMixin)

    //? if < 1.21.9 {

    /*// Cache: maps entitySolid/entityCutout Identifiers to entityTranslucent equivalents.
    private static final java.util.Map<net.minecraft.client.renderer.RenderType, net.minecraft.client.renderer.RenderType> solidToTranslucent
            = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        solidToTranslucent.put(
                net.minecraft.client.renderer.RenderType.entitySolid(Sheets.SHIELD_SHEET),
                net.minecraft.client.renderer.RenderType.entityTranslucent(Sheets.SHIELD_SHEET));
        solidToTranslucent.put(
                net.minecraft.client.renderer.RenderType.entitySolid(Sheets.BANNER_SHEET),
                net.minecraft.client.renderer.RenderType.entityTranslucent(Sheets.BANNER_SHEET));
        //? if < 1.21.4 {
        /^solidToTranslucent.put(
                net.minecraft.client.renderer.RenderType.entitySolid(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS),
                net.minecraft.client.renderer.RenderType.entityTranslucent(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
        ^///?}
    }

    public static net.minecraft.client.renderer.MultiBufferSource wrapTranslucentBufferSource(
            net.minecraft.client.renderer.MultiBufferSource original, float alpha) {
        return (net.minecraft.client.renderer.RenderType renderType) -> {
            var translucent = solidToTranslucent.get(renderType);
            if (translucent != null) {
                return original.getBuffer(translucent);
            }
            if (renderType == Sheets.cutoutBlockSheet()) {
                return original.getBuffer(Sheets.translucentItemSheet());
            }
            return original.getBuffer(renderType);
        };
    }

    public static void registerSolidToTranslucent(net.minecraft.client.renderer.RenderType solid, net.minecraft.client.renderer.RenderType translucent) {
        solidToTranslucent.putIfAbsent(solid, translucent);
    }
    *///? }
}
