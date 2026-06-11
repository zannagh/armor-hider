package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
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
        if (mod.isEmpty()) {
            return RenderInterceptionResult.ignore();
        }
        if (carrier.isPlayerFlying() || ArmorHiderClient.ET_LOADED) {
            return RenderInterceptionResult.ignore();
        }
        if (mod.shouldHide()) {
            cancel(ci);
            return new RenderInterceptionResult(true, true, getTargetScope(), carrier, mod);
        }
        return new RenderInterceptionResult(true, false, getTargetScope(), carrier, mod);
    }

    @Override
    public RenderInterceptionResult interceptFrom(@Nullable IdentityCarrier carrier, CallbackInfo ci) {
        return intercept(carrier, null, null, ci);
    }
}
