package de.zannagh.armorhider.client.render.rendertype;

import de.zannagh.armorhider.client.api.AhRenderTypeFactory;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

//? if > 1.21.10
//import net.minecraft.client.renderer.RenderTypes;

/**
 * Resolves version-specific translucent render types with depth writing disabled.
 * Depth writing is disabled so that semi-transparent armor does not occlude
 * translucent terrain (water, ice, stained-glass) behind it.
 */
public class RenderTypeFactory implements AhRenderTypeFactory {

    private boolean isEmpty;

    private static final AhRenderTypeFactory DEFAULT_FACTORY = new RenderTypeFactory();

    public RenderTypeFactory() {
        isEmpty = false;
    }

    public static AhRenderTypeFactory getInstance() {
        return DEFAULT_FACTORY;
    }

    public static RenderTypeFactory empty() {
        var instance = new RenderTypeFactory();
        instance.isEmpty = true;
        return instance;
    }

    public RenderType getTranslucentArmorRenderType(ResourceLocation texture) {
        if (isEmpty) {
            //? if >= 26.3-0.snapshot.2
            //return RenderTypes.entityTranslucent(texture);
            //? if > 1.21.10 && < 26.3-0.snapshot.2
            //return RenderTypes.armorTranslucent(texture);
            //? if <= 1.21.10 && > 1.21.1
            return RenderType.armorTranslucent(texture);
            //? if <= 1.21.1
            //return RenderType.armorCutoutNoCull(texture);
        }
        return ArmorHiderRenderTypes.translucentArmor(texture);
    }

    public RenderType getTranslucentEntityRenderType(ResourceLocation texture) {
        if (isEmpty) {
            //? if > 1.21.10
            //return RenderTypes.entityTranslucent(texture);
            //? if <= 1.21.10
            return RenderType.entityTranslucent(texture);
        }
        return ArmorHiderRenderTypes.translucentEntity(texture);
    }

    public RenderType getTranslucentArmorTrimRenderType(boolean decal) {
        if (isEmpty) {
            //? if >= 26.3-0.snapshot.2
            //return Sheets.translucentItemSheet();
            //? if > 1.20.1 && < 26.3-0.snapshot.2
            return Sheets.armorTrimsSheet(decal);
            //? if <= 1.20.1
            //return Sheets.armorTrimsSheet();
        }
        return ArmorHiderRenderTypes.translucentArmorTrim();
    }

    public RenderType getTranslucentItemSheetRenderType() {
        if (isEmpty) {
            return Sheets.translucentItemSheet();
        }
        return ArmorHiderRenderTypes.translucentItemSheet();
    }
}
