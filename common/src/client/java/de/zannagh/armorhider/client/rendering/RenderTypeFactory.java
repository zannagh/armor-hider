package de.zannagh.armorhider.client.rendering;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Resolves version-specific translucent render types with depth writing disabled.
 * Depth writing is disabled so that semi-transparent armor does not occlude
 * translucent terrain (water, ice, stained glass) behind it.
 */
public final class RenderTypeFactory {

    private RenderTypeFactory() {}

    public static RenderType translucentArmor(Identifier texture) {
        return ArmorHiderRenderTypes.translucentArmor(texture);
    }

    public static RenderType translucentEntity(Identifier texture) {
        return ArmorHiderRenderTypes.translucentEntity(texture);
    }

    public static RenderType translucentArmorTrim() {
        return ArmorHiderRenderTypes.translucentArmorTrim();
    }

    public static RenderType translucentItemSheet() {
        return ArmorHiderRenderTypes.translucentItemSheet();
    }
}
