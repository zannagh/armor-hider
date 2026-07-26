//? if uranus {
/*package de.zannagh.armorhider.client.mixin.compat.uranus;

import com.iafenvoy.uranus.client.render.armor.IArmorRendererBase;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Compatibility mixin for the Uranus library's custom armor rendering (used by Ice and Fire: CE and
 * other iafenvoy mods).
 * <p>
 * Uranus registers per-item armor renderers ({@link IArmorRendererBase}) and dispatches to them from
 * its own {@code HumanoidArmorLayer} mixin, drawing custom armor models via the interface's default
 * {@code render(...)} INSTEAD of vanilla {@code renderArmorPiece} — so armor-hider's renderArmorPiece
 * path never sees these pieces (the mod's scope trace showed only OFFHAND, never ARMOR_PIECE, for IAF
 * armor). Concrete renderers (e.g. Ice and Fire's {@code BasicArmorRenderer} / {@code ScaleArmorRenderer})
 * only override {@code getHumanoidArmorModel}, so this shared default {@code render} is the single hook
 * that covers every Uranus-based armor piece.
 * <p>
 * The default {@code render} both picks {@code RenderType.armorCutoutNoCull} and calls
 * {@code HumanoidModel.renderToBuffer(...color)} inline, so the full pipeline is handled here — enter the
 * ARMOR_PIECE scope on HEAD (cancel outright when fully hidden), swap to the translucent render type and
 * fade the model color while the scope is active, then exit on RETURN. Mirrors
 * {@code NeoForgeHumanoidArmorLayerMixin}. Injects into an interface default method, so the mixin is an
 * interface with private handlers.
 ^/
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(value = IArmorRendererBase.class, remap = false)
public interface UranusArmorRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void armorHider$enterUranusArmor(PoseStack poseStack, MultiBufferSource bufferSource,
            LivingEntity entity, EquipmentSlot slot, int packedLight, ItemStack item,
            HumanoidModel<?> model, CallbackInfo ci) {
        if (!(entity instanceof IdentityCarrier carrier)) {
            return;
        }
        var ctx = AhRenderManagementApi.enterScope(RenderScope.ARMOR_PIECE, carrier, slot, item);
        if (ctx.shouldCancel()) {
            AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "render",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    remap = true
            )
    )
    private RenderType armorHider$fadeUranusRenderType(Identifier texture, Operation<RenderType> original) {
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        var originalType = original.call(texture);
        if (modApi.getTranslucentArmorRenderType(texture, originalType) instanceof RenderType rt) {
            return rt;
        }
        return originalType;
    }

    @WrapOperation(
            method = "render",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
                    remap = true
            )
    )
    private void armorHider$fadeUranusColor(HumanoidModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer,
            int packedLight, int packedOverlay, int color, Operation<Void> original) {
        int modifiedColor = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE)
                .renderModificationApi().applyArmorTransparency(color);
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void armorHider$exitUranusArmor(PoseStack poseStack, MultiBufferSource bufferSource,
            LivingEntity entity, EquipmentSlot slot, int packedLight, ItemStack item,
            HumanoidModel<?> model, CallbackInfo ci) {
        AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
    }
}
*///?}
