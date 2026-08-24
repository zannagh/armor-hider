package de.zannagh.armorhider.client.mixin.compat.fabricapi;

import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.compat.FabricArmorRendererCompat;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >= 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?}

/**
 * Swaps the armor render type onto Armor Hider's translucent (depth-write-disabled) pipeline while a
 * Fabric API {@code ArmorRenderer} is drawing.
 *
 * <p>An {@code ArmorRenderer} builds its own draw, and every one of them reaches the same vanilla factory
 * for the type - {@code armorCutoutNoCull(texture)}. Hooking it here is what puts the texture in hand,
 * which is what building the translucent - or, under a shaderpack, the ordered-dither - counterpart needs;
 * the matching alpha for the vertex tint comes from {@link FabricArmorRendererGeometryMixin}.
 *
 * <p>This sits on a shared Minecraft entry point, so the guard has to be exact: it only acts inside the
 * window {@link FabricArmorRendererLayerMixin} opens around Fabric API's dispatch, and only when the
 * resolved modification actually asks for translucency. Vanilla armor (whose type swap lives in
 * {@code EquipmentRenderMixin} / {@code HumanoidArmorLayerMixin}) and every other caller keep the original
 * type. Without fabric-rendering-v1 the guard is a constant {@code false}.
 */
@Mixin(
        //? if >= 1.21.11
        RenderTypes.class
        //? if < 1.21.11
        //RenderType.class
)
public class FabricArmorRendererTypeMixin {

    @Inject(method = "armorCutoutNoCull", at = @At("RETURN"), cancellable = true)
    private static void armorHider$translucentCustomArmorType(Identifier texture,
            CallbackInfoReturnable<RenderType> cir) {
        if (!FabricArmorRendererCompat.isDrawingCustomArmor()) {
            return;
        }
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty() || !ctx.needsTranslucency()) {
            return;
        }
        if (ctx.renderModificationApi().getTranslucentArmorRenderType(texture, cir.getReturnValue())
                instanceof RenderType translucent) {
            cir.setReturnValue(translucent);
        }
    }
}
