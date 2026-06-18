package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Default renderer for {@link RenderScope#OFFHAND}. Used both by the first-person {@code ItemInHandRenderer}
 * setup and the third-person {@code ItemInHandLayer}. The slot/stack are derived from the carrier's
 * offhand if no explicit stack is provided.
 */
public class ArmorHiderOffhandRenderer extends AbstractArmorHiderRenderer {

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.OFFHAND;
    }

    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        if (stack != null) {
            if (stack.is(Items.AIR)) {
                return RenderInterceptionResult.ignore();
            }
        }
        if (slot == null) {
            slot = EquipmentSlot.OFFHAND;
        }
        IdentityCarrier carrier = identityCarrier instanceof IdentityCarrier ic ? ic : null;
        if (carrier == null) {
            resolveModification(null, null, null);
            return RenderInterceptionResult.ignore();
        }
        ItemStack offhand = stack != null ? stack : carrier.getItemBySlot(slot);
        if (offhand.isEmpty()) {
            resolveModification(carrier, slot, ItemStack.EMPTY);
            return RenderInterceptionResult.ignore();
        }

        if (carrier.isPlayerBlocking()
          && ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().showShieldWhenBlocking.getValue()) {
            return RenderInterceptionResult.ignore();
        }

        return standardIntercept(carrier, slot, offhand, ci);
    }
}
