package de.zannagh.armorhider.client.mixin.compat.fabricapi;

import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.compat.FabricArmorRendererCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

//? if >= 1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollection;
//?}

//? if < 1.21.9 {
/*import net.minecraft.client.model.geom.ModelPart;
*///?}

/**
 * Applies Armor Hider's opacity to armor drawn through Fabric API's {@code ArmorRenderer}.
 *
 * <p>{@link FabricArmorRendererLayerMixin} brackets Fabric API's dispatch with a render scope and cancels
 * a fully hidden piece outright; {@link FabricArmorRendererTypeMixin} puts a partly hidden one on the
 * translucent pipeline. This supplies the third ingredient - the alpha in the vertex tint - without which
 * a translucent render type still draws at full opacity.
 *
 * <p>The hook deliberately sits on the <b>vanilla</b> draw seam a mod's renderer necessarily reaches,
 * rather than on Fabric API's own {@code ArmorRenderer.renderPart} /
 * {@code ArmorRenderer.submitTransformCopyingModel} helpers: those are static methods on an interface and
 * Mixin does not resolve them as injection targets (it reports "Scanned 0 target(s)"). The vanilla seam
 * also covers renderers that build their draw by hand instead of going through the helpers. Concretely:
 * <ul>
 *   <li>1.21.9+ - {@code SubmitNodeCollection.submitModel}, the single implementation every model submit
 *       funnels through, including Minecraft's own untinted convenience overload - which is what Fabric
 *       API's untinted helper calls, so an unmodified opaque white tint is exactly what arrives here.</li>
 *   <li>before 1.21.9 - {@code ModelPart.render}, the leaf every model draw ends at. The same seam
 *       {@code hand.ModelPartMixin} uses for the offhand and head scopes.</li>
 * </ul>
 *
 * <p>Gated on {@link FabricArmorRendererCompat#isDrawingCustomArmor()}, so on these very hot paths the
 * cost outside a Fabric custom armor draw is one constant-folded {@code null} check, and no unrelated draw
 * can pick up the armor scope's alpha.
 *
 * <p>Known gap: the enchantment-glint toggle does not reach these renderers. A mod decides whether to draw
 * a glint layer inside its own {@code render}, and there is no vanilla seam in between to answer that from
 * - so a glinting Fabric-rendered piece keeps its glint. It still disappears with the piece when the slot
 * is hidden, and fades along with it below full opacity.
 */
@Mixin(
        //? if >= 1.21.9
        SubmitNodeCollection.class
        //? if < 1.21.9
        //ModelPart.class
)
public class FabricArmorRendererGeometryMixin {

    //? if >= 1.21.9 {
    // Arg ordinals among the ints: light = 0, overlay = 1, tintedColor = 2, outlineColor = 3.
    @ModifyVariable(method = "submitModel", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private int armorHider$fadeCustomArmorTint(int tintedColor) {
        if (!FabricArmorRendererCompat.isDrawingCustomArmor()) {
            return tintedColor;
        }
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE)
                .renderModificationApi().applyArmorTransparency(tintedColor);
    }
    //? } elif >= 1.21 {
    /*@ModifyVariable(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"),
            ordinal = 2,
            argsOnly = true
    )
    private int armorHider$fadeCustomArmorTint(int color) {
        if (!FabricArmorRendererCompat.isDrawingCustomArmor()) {
            return color;
        }
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE)
                .renderModificationApi().applyArmorTransparency(color);
    }
    *///? } else {
    /*@ModifyVariable(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"),
            ordinal = 3,
            argsOnly = true
    )
    private float armorHider$fadeCustomArmorAlpha(float alpha) {
        if (!FabricArmorRendererCompat.isDrawingCustomArmor()) {
            return alpha;
        }
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        return ctx.isEmpty() ? alpha : alpha * ctx.renderModificationApi().getTransparencyAlpha();
    }
    *///? }
}
