// FGM 5.0.0-Beta.5+ (the 26.1.2+ pins, new package layout): render classes live in com.wildfire.client.render
// and the breast armor is drawn through vanilla EquipmentLayerRenderer.renderLayers. Builds up to Beta.4 are
// handled by GenderArmorLayerMixin.
//? if gender && >= 26.1.2 {
package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wildfire.client.render.BreastSide;
import com.wildfire.client.render.GenderArmorLayer;
import com.wildfire.client.render.GenderRenderState;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.compat.ArmoredElytraCompat;
import de.zannagh.armorhider.client.compat.GenderBreastRenderGuard;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compatibility mixin for Wildfire's Female Gender Mod 5.0.0-Beta.5 and later.
 * <p>
 * Beta.5 collapsed {@code renderBreastArmor}/{@code renderArmorTrim} into a single private
 * {@code renderArmor(state, renderState, poseStack, collector, light, overlay, side)} (called once per
 * side) that hands FGM's breast box to vanilla {@code EquipmentLayerRenderer.renderLayers}. Everything
 * Armor Hider does to a body chestplate inside that call - hide, alpha, translucent render type, trims,
 * glint, render order - is already applied by {@code EquipmentRenderMixin} whenever an ARMOR_PIECE (or
 * ELYTRA) scope is active. So this mixin only decides the interception for the chest slot and brackets the
 * whole {@code renderArmor} call with that scope, closing it on every exit path (Beta.5's
 * {@code renderArmor} has throw sites after the entry: {@code Objects.requireNonNull} on the EQUIPPABLE
 * component and {@code Optional.orElseThrow} on the asset id - a plain RETURN inject would leak the scope
 * for the rest of the frame on those).
 * <p>
 * {@link GenderBreastRenderGuard} is raised for the duration of the draw so {@code EquipmentRenderMixin}
 * can tell the breast draw apart from a body plate or a foreign elytra (see there).
 */
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(value = GenderArmorLayer.class, remap = false)
public class GenderArmorLayerV5Mixin {

    // Armored Elytra (dorkix) compat. FGM's renderArmor reads the worn chest item's EQUIPPABLE component and
    // skips the breast armor when that asset has no humanoid layer. A dorkix armored elytra is a plain
    // Items.ELYTRA (chestplate stashed in CUSTOM_DATA), whose asset is wings-only, so FGM never drew breast
    // armor for it. Substitute the stored chestplate for that lookup (WildfireHelperMixin already makes
    // FGM's armor config resolve to the chestplate). Non-armored-elytra items pass through unchanged.
    // require = 0 keeps this @Pseudo compat a silent no-op if FGM's shape drifts;
    // ArmoredElytraGenderSmokeTest pins the target.
    @WrapOperation(
            method = "renderArmor",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;",
                    remap = true)
    )
    private Object armorHider$armoredElytraEquippable(ItemStack instance, DataComponentType<?> type, Operation<Object> original) {
        if (type == DataComponents.EQUIPPABLE) {
            ItemStack chestplate = ArmoredElytraCompat.underlyingChestplateOrSelf(instance);
            if (chestplate != instance) {
                return original.call(chestplate, type);
            }
        }
        return original.call(instance, type);
    }

    @WrapMethod(method = "renderArmor", require = 0)
    private void armorHider$wrapRenderArmor(HumanoidRenderState state, GenderRenderState renderState, PoseStack poseStack,
                                            SubmitNodeCollector collector, int light, int overlay, BreastSide side,
                                            Operation<Void> original) {
        // Combat detection is deliberately NOT short-circuited here: SlotModification.of applies the combat
        // opacity ramp, so the breast armor stays in lockstep with the body chestplate on its own.
        //
        // Armored Elytra (dorkix): the worn chest item is a plain Items.ELYTRA, so feed the interception the
        // stored chestplate - the breast armor IS that chestplate's armor and must follow the chest slider
        // (not opacityAffectElytra), exactly like the body plate dorkix swaps into HumanoidArmorLayer.
        ItemStack effectiveStack = ArmoredElytraCompat.underlyingChestplateOrSelf(state.chestEquipment);
        // The interception API reports "hide" through both the result and CallbackInfo#cancel; there is no
        // real callback here, so hand it a throwaway one and act on the result.
        CallbackInfo ci = new CallbackInfo("renderArmor", true);
        var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.ARMOR_PIECE)
                .intercept(state, EquipmentSlot.CHEST, effectiveStack, ci);
        if (result.shouldIntercept() && result.shouldCancel()) {
            // Hidden: draw nothing. Exit BOTH scopes - an "Elytra Armor" datapack chest (an Items.ELYTRA
            // that also carries a chestplate asset) makes the ARMOR_PIECE renderer delegate to the ELYTRA
            // renderer, so the scope it may have entered before cancelling can be either.
            AhRenderManagementApi.exitScopes(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
            return;
        }
        if (result.shouldIntercept()) {
            AhRenderManagementApi.enterScope(result);
        }
        GenderBreastRenderGuard.enter();
        try {
            original.call(state, renderState, poseStack, collector, light, overlay, side);
        } finally {
            GenderBreastRenderGuard.exit();
            AhRenderManagementApi.exitScopes(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
        }
    }
}
//?}
