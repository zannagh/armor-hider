//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.compat.curios;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.compat.AccessoryHidingCompat;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Compat for the Curios accessory provider (issue #246). Curios draws every equipped accessory from
 * {@code CuriosLayer.submit(...)}, which calls {@code ICurioRenderer.render(stack, slotContext, ...)}
 * once per curio. Wrapping that call lets Armor Hider skip a curio's render — the only way to hide an
 * accessory generically, since {@code ICurioRenderer.render} exposes no colour/alpha to fade it.
 * <p>
 * {@code SlotContext.identifier()} (resolved reflectively in {@link AccessoryHidingCompat}) gives the
 * accessory slot type ({@code head}, {@code necklace}, {@code belt}, {@code feet}, …); the render state
 * is the wearer. Because Artifacts renders through Curios on NeoForge, this hook also covers Artifacts.
 * <p>
 * {@code @Pseudo} + {@code remap = false} + {@code require = 0}: Curios is an optional NeoForge-only mod
 * (mojmap runtime, so the Minecraft descriptors match without remapping); absent on Fabric, where the
 * mixin is simply skipped.
 */
@Pseudo
@Mixin(targets = "top.theillusivec4.curios.client.CuriosLayer", remap = false)
public class CuriosLayerMixin {

    @WrapOperation(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ltop/theillusivec4/curios/api/client/ICurioRenderer;render(Lnet/minecraft/world/item/ItemStack;Ltop/theillusivec4/curios/api/SlotContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lnet/minecraft/client/renderer/entity/RenderLayerParent;Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;FF)V"
            ),
            remap = false,
            require = 0
    )
    private void armorHider$maybeHideCurio(@Coerce Object renderer,
                                           ItemStack stack,
                                           @Coerce Object slotContext,
                                           PoseStack poseStack,
                                           SubmitNodeCollector collector,
                                           int light,
                                           LivingEntityRenderState renderState,
                                           @Coerce Object renderLayerParent,
                                           @Coerce Object context,
                                           float partialA,
                                           float partialB,
                                           Operation<Void> original) {
        if (AccessoryHidingCompat.shouldHideCurio(slotContext, renderState)) {
            return;
        }
        original.call(renderer, stack, slotContext, poseStack, collector, light, renderState, renderLayerParent, context, partialA, partialB);
    }
}
//?}
