package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Default renderer for {@link RenderScope#HEAD}.
 * <p>
 * The "head item" is the worn item picked up via {@link IdentityCarrier#customHeadItem()} rather than the
 * helmet slot — skulls and head-mounted blocks are tracked separately from the armor pipeline.
 */
public class ArmorHiderHeadRenderer extends AbstractArmorHiderRenderer {

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.HEAD;
    }

    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        IdentityCarrier carrier = identityCarrier instanceof IdentityCarrier ic ? ic : null;
        if (carrier == null) {
            resolveModification(null, null, null);
            return RenderInterceptionResult.ignore();
        }
        ItemStack headItem = stack != null ? stack : carrier.customHeadItem();
        if (headItem == null || headItem.isEmpty()) {
            resolveModification(carrier, EquipmentSlot.HEAD, ItemStack.EMPTY);
            return RenderInterceptionResult.ignore();
        }
        return standardIntercept(carrier, EquipmentSlot.HEAD, headItem, ci);
    }

    @Override
    public RenderInterceptionResult interceptFrom(@Nullable IdentityCarrier carrier, CallbackInfo ci) {
        return intercept(carrier, null, null, ci);
    }
}
