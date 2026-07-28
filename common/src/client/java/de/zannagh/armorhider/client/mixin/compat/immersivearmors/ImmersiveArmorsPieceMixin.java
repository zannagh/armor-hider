//? if immersivearmors {
package de.zannagh.armorhider.client.mixin.compat.immersivearmors;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

//? if >= 26.1-0.snapshot {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?}

//? if < 26.1-0.snapshot {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

/**
 * Compatibility mixin for the concrete Immersive Armors piece types.
 *
 * <p>Immersive Armors draws an armor item as a list of {@code Piece}s, each of which implements
 * {@code render}. This mixin is applied to every concrete implementation so that a render scope is
 * opened around each piece, carrying the slot, item and player identity that Armor Hider needs to
 * resolve opacity, item exclusions and combat fading.
 *
 * <p>The scope is deliberately self-contained rather than inherited from Armor Hider's own
 * {@code HumanoidArmorLayerMixin}. Immersive Armors cancels the vanilla equipment layer at HEAD, and
 * the relative order of two HEAD injections from different mods is not guaranteed: if Armor Hider's
 * hook wins the race it opens a scope whose RETURN teardown is then skipped by the cancel. Opening
 * and closing the scope here makes the outcome identical either way and re-closes a scope leaked by
 * that race.
 *
 * <p>Base-class geometry (colour, render type, glint) is handled in
 * {@link ImmersiveArmorsPieceGeometryMixin}.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = {
        "immersive_armors.client.render.entity.piece.LayerPiece",
        "immersive_armors.client.render.entity.piece.ModelPiece",
        "immersive_armors.client.render.entity.piece.GearPiece",
        "immersive_armors.client.render.entity.piece.CapePiece",
        "immersive_armors.client.render.entity.piece.ItemPiece"
}, remap = false)
public class ImmersiveArmorsPieceMixin {

    // ========================
    // Scope lifecycle
    // ========================

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    //? if >= 26.1-0.snapshot
    private void armorHider$enterPiece(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidRenderState renderState, ItemStack itemStack, float tickDelta, EquipmentSlot armorSlot, HumanoidModel<?> armorModel, int order, CallbackInfoReturnable<Integer> cir) {
    //? if < 26.1-0.snapshot
    //private void armorHider$enterPiece(PoseStack poseStack, MultiBufferSource bufferSource, int light, LivingEntity entity, ItemStack itemStack, float tickDelta, EquipmentSlot armorSlot, HumanoidModel<?> armorModel, CallbackInfo ci) {

        //? if >= 26.1-0.snapshot
        Object identitySource = renderState;
        //? if < 26.1-0.snapshot
        //Object identitySource = entity;

        if (!(identitySource instanceof IdentityCarrier carrier)) {
            return;
        }
        var ctx = AhRenderManagementApi.enterScope(RenderScope.ARMOR_PIECE, carrier, armorSlot, itemStack);
        if (ctx.shouldCancel()) {
            AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
            //? if >= 26.1-0.snapshot
            cir.setReturnValue(order);
            //? if < 26.1-0.snapshot
            //ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    //? if >= 26.1-0.snapshot
    private void armorHider$exitPiece(CallbackInfoReturnable<Integer> cir) {
    //? if < 26.1-0.snapshot
    //private void armorHider$exitPiece(CallbackInfo ci) {
        AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
    }

    // ========================
    // Render types owned by the subclasses
    // ========================
    //
    // Gear and cape geometry never reaches Piece#renderParts — both hardcode armorCutoutNoCull in
    // their own class — so the translucent swap has to be repeated here. `require = 0` keeps this
    // inert on the piece types that carry no such call, and across the Immersive Armors versions
    // that emit the cape from `render` rather than a separate `renderCape`.

    //? if >= 26.1-0.snapshot {
    @WrapOperation(
            method = {"render", "renderCape"},
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    remap = true)
    )
    private RenderType armorHider$translucentSubPieceRenderType(Identifier texture, Operation<RenderType> original) {
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
            method = {"render", "renderCape"},
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    remap = true)
    )
    private RenderType armorHider$translucentSubPieceRenderType(Identifier texture, Operation<RenderType> original) {
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        var originalType = original.call(texture);
        if (modApi.getTranslucentArmorRenderType(texture, originalType) instanceof RenderType rt) {
            return rt;
        }
        return originalType;
    }
    *///?}

    // ========================
    // Colour owned by the subclasses (pre-26.1 only)
    // ========================
    //
    // From 26.1 gear/cape geometry goes through Piece#renderGeometry and is faded there. Before
    // that, both draw straight from their own class, so the alpha has to be substituted at the draw
    // call they use.
    //
    // Known gap, pre-26.1 only: cape geometry keeps its opacity. CapePiece draws through
    // `CapeModel`, an Immersive Armors class, and an @At target cannot be written for it in a
    // remap-safe way — the owner is a mod class (so `remap = true` cannot resolve it) while the
    // descriptor is all Minecraft types (so `remap = false` matches dev but not a released,
    // intermediary-mapped jar). Silently emitting a target that only binds in dev is worse than the
    // gap, so it is left uncovered. Full-hide still works on capes there, because it is the whole
    // `render` call that gets cancelled; only partial transparency is affected. From 26.1 onward
    // capes fade normally.

    //? if >= 1.21 && < 26.1-0.snapshot {
    /*@WrapOperation(
            method = "render",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
                    remap = true)
    )
    private void armorHider$fadeSubPiecePartColor(ModelPart part, PoseStack poseStack, VertexConsumer vertexConsumer,
            int packedLight, int packedOverlay, int color, Operation<Void> original) {
        original.call(part, poseStack, vertexConsumer, packedLight, packedOverlay,
                AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE)
                        .renderModificationApi().applyArmorTransparency(color));
    }
    *///?}

    //? if < 1.21 {
    /*@WrapOperation(
            method = "render",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
                    remap = true)
    )
    private void armorHider$fadeSubPiecePartAlpha(ModelPart part, PoseStack poseStack, VertexConsumer vertexConsumer,
            int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
            Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) {
            original.call(part, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }
        original.call(part, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue,
                ctx.renderModificationApi().getTransparencyAlpha());
    }
    *///?}
}
//?}
