package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Default renderer for {@link RenderScope#ELYTRA}.
 * <p>
 * Short-circuits:
 * <ul>
 *   <li>player flying → don't enter the elytra scope at all (the body re-renders the elytra geometry);</li>
 *   <li>ElytraTrims loaded → don't enter the scope so ET's own pipeline drives rendering;</li>
 *   <li>modification {@code shouldHide} → cancel and signal.</li>
 * </ul>
 */
public class ArmorHiderElytraRenderer extends AbstractArmorHiderRenderer {

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.ELYTRA;
    }

    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        IdentityCarrier carrier = identityCarrier instanceof IdentityCarrier ic ? ic : null;
        if (carrier == null) {
            resolveModification(null, null, null);
            return RenderInterceptionResult.ignore();
        }
        ItemStack elytraStack = stack != null ? stack : ItemsUtil.ELYTRA_ITEM_STACK;
        var mod = resolveModification(carrier, EquipmentSlot.CHEST, elytraStack);
        RenderInterceptionResult result;
        // Flying must short-circuit BEFORE shouldHide so that 0%-opacity players still see the
        // elytra geometry while actually elytra-flying — the wings are the flight indicator.
        if (mod.isEmpty()) {
            result = RenderInterceptionResult.ignore(getTargetScope(), mod);
        }
        else if (carrier.isPlayerFlying()) {
            mod = new SlotModification(EquipmentSlot.CHEST, false, false, false, 1D, carrier.armorHider$playerName(), null, new ItemInfo(stack));
            result = RenderInterceptionResult.ignore(getTargetScope(), mod);
        }
        // With ElytraTrims present, ET's own rendering pipeline owns elytra appearance. Submitting
        // a partial-alpha scope here lets unrelated sliders (e.g. head) leak into ET's submissions
        // via SubmitNodeCollectorMixin's scope checks — so cap to full-hide-or-vanilla.
        else if (ArmorHiderClient.ET_LOADED) {
            if (mod.shouldHide()) {
                cancel(ci);
                result = new RenderInterceptionResult(true, true, getTargetScope(), carrier, mod);
            } else {
                result = RenderInterceptionResult.ignore(getTargetScope(), mod);
            }
        }
        else if (mod.shouldHide()) {
            cancel(ci);
            result = new RenderInterceptionResult(true, true, getTargetScope(), carrier, mod);
        }
        else {
            result = new RenderInterceptionResult(true, false, getTargetScope(), carrier, mod);
        }
        AhRenderManagementApi.enterScope(result);
        return result;
    }
}
