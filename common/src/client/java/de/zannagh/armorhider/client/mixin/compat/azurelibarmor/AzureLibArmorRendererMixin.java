//? if < 1.21 {
/*package de.zannagh.armorhider.client.mixin.compat.azurelibarmor;

import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/^*
 * Compatibility mixin for AzureLib Armor ({@code azurelibarmor}), the GeckoLib fork the 1.20.1 RPG Series
 * mods (Wizards 1.x, Elemental Wizards 1.x and siblings) draw their armor with.
 *
 * <p>AzureLib Armor swaps its {@code GeoArmorRenderer} in as the armor model inside vanilla
 * {@code HumanoidArmorLayer.renderArmorPiece}, so Armor Hider's own layer mixin already opens the
 * {@code ARMOR_PIECE} scope and cancels a fully hidden piece. But the renderer's {@code renderToBuffer}
 * ignores the vertex consumer and alpha vanilla hands it: it picks its own {@code armorCutoutNoCull} type
 * through {@code getRenderType} and draws at the colour from {@code getRenderColor} (opaque white), so a
 * partly hidden piece stays fully opaque. This supplies those two pieces while the scope is active.
 *
 * <p>Both target names also match the synthetic bridge overloads ({@code GeoAnimatable}-typed), which call
 * through to the real method. The render type hook cancels at HEAD, so the bridge short-circuits; the alpha
 * hook clamps rather than multiplies, so running on both is a no-op the second time.
 ^/
@Pseudo
@Mixin(targets = "mod.azure.azurelibarmor.renderer.GeoArmorRenderer", remap = false)
public class AzureLibArmorRendererMixin {

    @Inject(method = "getRenderType*", at = @At("HEAD"), cancellable = true, require = 0)
    private void armorHider$translucentAzureArmorType(@Coerce Object animatable, Identifier texture,
            MultiBufferSource bufferSource, float partialTick, CallbackInfoReturnable<RenderType> cir) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty() || !ctx.needsTranslucency()) {
            return;
        }
        if (ctx.renderModificationApi().getTranslucentArmorRenderType(texture, RenderType.armorCutoutNoCull(texture))
                instanceof RenderType translucent) {
            cir.setReturnValue(translucent);
        }
    }

    // Float args of actuallyRender: partialTick = 0, red = 1, green = 2, blue = 3, alpha = 4.
    @ModifyVariable(method = "actuallyRender", at = @At("HEAD"), ordinal = 4, argsOnly = true, require = 0)
    private float armorHider$fadeAzureArmorAlpha(float alpha) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) {
            return alpha;
        }
        return Math.min(alpha, ctx.renderModificationApi().getTransparencyAlpha());
    }
}
*///?}
