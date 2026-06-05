//? if mekanism {
/*package de.zannagh.armorhider.client.mixin.compat.mekanism;

import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.render.RenderScope;
import mekanism.client.render.armor.MekaSuitArmor;
import mekanism.common.lib.Color;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(value = MekaSuitArmor.class, remap = false)
public class MekaSuitArmorMixin {

    @ModifyVariable(method = "renderMekaSuit", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private Color applyTransparencyToColor(Color color) {
        var armorCtx = ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE);
        if (!armorCtx.isEmpty() && armorCtx.modification().transparency() < 1.0) {
            return color.alpha(color.ad() * armorCtx.modification().transparency());
        }
        return color;
    }
}
*///?}
