//? if immersivearmors {
package de.zannagh.armorhider.client.mixin.compat.immersivearmors;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 26.1-0.snapshot {
import org.spongepowered.asm.mixin.injection.ModifyVariable;
//?}

//? if >= 1.21 && < 26.1-0.snapshot {
/*import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///?}

//? if < 1.21 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
*///?}

/**
 * Compatibility mixin for Immersive Armors' {@code Piece} base class.
 *
 * <p>Immersive Armors replaces vanilla armor rendering wholesale: its own
 * {@code MixinHumanoidArmorLayer} cancels {@code HumanoidArmorLayer.renderArmorPiece} at HEAD for
 * every {@code ExtendedArmorItem} and draws a list of {@code Piece}s instead. That means none of
 * Armor Hider's vanilla equipment-layer hooks ever see these items — the piece geometry has to be
 * faded where Immersive Armors actually emits it.
 *
 * <p>This mixin owns the two levers that live on the shared base class:
 * <ul>
 *   <li>the packed ARGB colour handed to the geometry, and</li>
 *   <li>the {@code armorCutoutNoCull} render type, which discards partial alpha and therefore has
 *       to be swapped for a translucent one before a fade is visible at all.</li>
 * </ul>
 *
 * <p>Scope setup/teardown and the piece types that bypass the base class live in
 * {@link ImmersiveArmorsPieceMixin}.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = "immersive_armors.client.render.entity.piece.Piece", remap = false)
public class ImmersiveArmorsPieceGeometryMixin {

    // ========================
    // Colour / alpha
    // ========================

    //? if >= 26.1-0.snapshot {
    /**
     * On 26.1+ every non-item piece funnels through {@code Piece#renderGeometry} — layer pieces,
     * deco models, gears, capes, armor trims and the glint pass all end up here. Fading the colour
     * argument at this single choke point covers the lot.
     *
     * <p>{@code renderGeometry} is overloaded (the 8-arg form delegates to the 9-arg form), so this
     * runs twice for the delegating path. That is harmless: {@code applyArmorTransparency} sets an
     * absolute alpha via {@code ColorMath.withAlpha} rather than scaling it, so it is idempotent.
     */
    @ModifyVariable(method = "renderGeometry", at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private int armorHider$fadePieceColor(int color) {
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE)
                .renderModificationApi().applyArmorTransparency(color);
    }
    //?}

    //? if >= 1.21 && < 26.1-0.snapshot {
    /*/^*
     * Pre-26.1 there is no {@code renderGeometry} funnel: {@code renderParts} draws layer and deco
     * pieces directly. Gear, cape and trim geometry is emitted by the subclasses and is handled in
     * {@link ImmersiveArmorsPieceMixin}.
     ^/
    @ModifyVariable(method = "renderParts", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int armorHider$fadePieceColor(int color) {
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE)
                .renderModificationApi().applyArmorTransparency(color);
    }
    *///?}

    //? if < 1.21 {
    /*/^*
     * On 1.20.1 {@code renderParts} carries loose red/green/blue floats and hardcodes alpha to 1.0,
     * so there is no colour argument to fade — the alpha has to be substituted at the draw call.
     ^/
    @WrapOperation(
            method = "renderParts",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
                    remap = true)
    )
    private void armorHider$fadePieceAlpha(EntityModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer,
            int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
            Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) {
            original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue,
                ctx.renderModificationApi().getTransparencyAlpha());
    }
    *///?}

    // ========================
    // Render type
    // ========================
    //
    // armorCutoutNoCull discards partial alpha, so the colour fade above is invisible until the
    // render type is swapped for a translucent one. The entityTranslucent and beaconBeam branches
    // Immersive Armors picks for its `translucent()`/`glowing()` pieces already blend, so only the
    // cutout default needs intercepting.

    //? if >= 26.1-0.snapshot {
    @WrapOperation(
            method = "renderParts",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    remap = true)
    )
    private RenderType armorHider$translucentPieceRenderType(Identifier texture, Operation<RenderType> original) {
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        var originalType = original.call(texture);
        if (modApi.getTranslucentArmorRenderType(texture, originalType) instanceof RenderType rt) {
            return rt;
        }
        return originalType;
    }
    //?}

    //? if < 26.1-0.snapshot {
    /*@WrapOperation(
            method = "renderParts",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    remap = true)
    )
    private RenderType armorHider$translucentPieceRenderType(Identifier texture, Operation<RenderType> original) {
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        var originalType = original.call(texture);
        if (modApi.getTranslucentArmorRenderType(texture, originalType) instanceof RenderType rt) {
            return rt;
        }
        return originalType;
    }
    *///?}

    // ========================
    // Glint
    // ========================
    //
    // The glint predicate is `hasGlint() || itemStack.hasFoil() && <config>`, i.e. a piece can be
    // permanently glinting regardless of enchantments. Both halves are suppressed so a hidden or
    // faded piece does not keep a fully opaque foil overlay.

    @ModifyExpressionValue(
            method = "renderParts",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z",
                    remap = true)
    )
    private boolean armorHider$suppressPieceItemGlint(boolean original) {
        return armorHider$glint(original);
    }

    @ModifyExpressionValue(
            method = "renderParts",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Limmersive_armors/client/render/entity/piece/Piece;hasGlint()Z",
                    remap = false)
    )
    private boolean armorHider$suppressPieceIntrinsicGlint(boolean original) {
        return armorHider$glint(original);
    }

    @Unique
    private boolean armorHider$glint(boolean original) {
        if (!original) {
            return false;
        }
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) {
            return true;
        }
        if (ctx.modification().shouldHide()) {
            return false;
        }
        return ctx.renderModificationApi().getHasFoil(true);
    }
}
//?}
