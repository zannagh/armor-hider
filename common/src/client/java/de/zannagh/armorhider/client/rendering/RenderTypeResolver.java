package de.zannagh.armorhider.client.rendering;

import net.minecraft.client.renderer.Sheets;

//?if >= 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
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
 * Resolves version-specific translucent render types.
 * Isolates all Stonecutter version conditionals for RenderType creation.
 */
public final class RenderTypeResolver {

    private RenderTypeResolver() {}

    //? if >= 1.21.11
    public static RenderType translucentArmor(Identifier texture) { return RenderTypes.armorTranslucent(texture); }
    //? if >= 1.21.4 && < 1.21.11
    //public static RenderType translucentArmor(ResourceLocation texture) { return RenderType.armorTranslucent(texture); }
    //? if < 1.21.4
    //public static RenderType translucentArmor(ResourceLocation texture) { return RenderType.entityTranslucent(texture); }

    //? if >= 1.21.11
    public static RenderType translucentEntity(Identifier texture) { return RenderTypes.entityTranslucent(texture); }
    //? if >= 1.21.4 && < 1.21.11
    //public static RenderType translucentEntity(ResourceLocation texture) { return RenderType.entityTranslucent(texture); }
    //? if < 1.21.4
    //public static RenderType translucentEntity(ResourceLocation texture) { return RenderType.entityTranslucent(texture); }

    //? if >= 1.21.11
    public static RenderType translucentArmorTrim() { return RenderTypes.armorTranslucent(Sheets.ARMOR_TRIMS_SHEET); }
    //? if >= 1.21.4 && < 1.21.11
    //public static RenderType translucentArmorTrim() { return RenderType.armorTranslucent(Sheets.ARMOR_TRIMS_SHEET); }
    //? if < 1.21.4
    //public static RenderType translucentArmorTrim() { return RenderType.entityTranslucent(net.minecraft.client.renderer.Sheets.ARMOR_TRIMS_SHEET); }

    public static RenderType translucentItemSheet() {
        //? if >= 1.21.11
        return Sheets.translucentBlockItemSheet();
        //? if < 1.21.11
        //return Sheets.translucentItemSheet();
    }
}
