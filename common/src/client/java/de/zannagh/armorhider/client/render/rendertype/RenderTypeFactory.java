package de.zannagh.armorhider.client.render.rendertype;

import de.zannagh.armorhider.client.api.AhRenderTypeFactory;import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Resolves version-specific translucent render types with depth writing disabled.
 * Depth writing is disabled so that semi-transparent armor does not occlude
 * translucent terrain (water, ice, stained glass) behind it.
 */
public class RenderTypeFactory implements AhRenderTypeFactory {

    public RenderType getTranslucentArmorRenderType(Identifier texture) {
        return ArmorHiderRenderTypes.translucentArmor(texture);
    }

    public RenderType getTranslucentEntityRenderType(Identifier texture) {
        return ArmorHiderRenderTypes.translucentEntity(texture);
    }

    public RenderType getTranslucentArmorTrimRenderType() {
        return ArmorHiderRenderTypes.translucentArmorTrim();
    }

    public RenderType getTranslucentItemSheetRenderType() {
        return ArmorHiderRenderTypes.translucentItemSheet();
    }
}
