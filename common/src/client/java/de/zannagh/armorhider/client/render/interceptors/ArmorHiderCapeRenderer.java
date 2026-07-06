package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.log.DebugLogger;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Default renderer for {@link RenderScope#CAPE}.
 * <p>
 * The cape is a special case: even when the chest piece is "hidden" we usually still want the cape
 * to render — only the elytra-while-flying combination requires suppressing the cape entirely (the
 * elytra-hidden body re-renders the cape itself).
 */
public class ArmorHiderCapeRenderer extends AbstractArmorHiderRenderer {

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.CAPE;
    }

    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        IdentityCarrier carrier = identityCarrier instanceof IdentityCarrier ic ? ic : null;
        if (carrier == null) {
            setEmptyModification();
            return RenderInterceptionResult.ignore();
        }
        ItemStack chest = stack != null ? stack : carrier.armorHider$getItemBySlot(EquipmentSlot.CHEST);
        var mod = resolveModification(carrier, EquipmentSlot.CHEST, chest);
        if (mod.isEmpty()) {
            return RenderInterceptionResult.ignore();
        }
        if (mod.shouldHide() && new ItemInfo(chest).isElytra() && carrier.isPlayerFlying()) {
            DebugLogger.log("CapeRendering: Player is flying with hidden elytra, suppressing cape rendering temporarily.");
            cancel(ci);
            return new RenderInterceptionResult(true, true, getTargetScope(), carrier, mod);
        }
        return new RenderInterceptionResult(true, false, getTargetScope(), carrier, mod);
    }
}
