package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Default renderer for {@link RenderScope#ARMOR_PIECE}. Drives the {@code EquipmentLayerRenderer} /
 * {@code HumanoidArmorLayer} interception path: builds the modification from the slot+stack the caller
 * passes in, and carves out the elytra-while-flying case (cape/body handle that combo elsewhere).
 */
public class ArmorHiderItemRenderer extends AbstractArmorHiderRenderer {

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.ARMOR_PIECE;
    }

    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        IdentityCarrier carrier = identityCarrier instanceof IdentityCarrier ic ? ic : null;
        EquipmentSlot resolvedSlot = slot;
        if (resolvedSlot == null && stack != null) {
            resolvedSlot = new ItemInfo(stack).getEquippableSlot();
        }

        if (stack != null) {
            if (new ItemInfo(stack).isElytra()) {
                var elytraRenderer = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.ELYTRA);
                return elytraRenderer.intercept(identityCarrier, resolvedSlot, stack, ci);
            }
        }
        else if (resolvedSlot == EquipmentSlot.CHEST) {
            if (carrier != null && new ItemInfo(carrier.getItemBySlot(resolvedSlot)).isElytra()){
                var elytraRenderer = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.ELYTRA);
                return elytraRenderer.intercept(identityCarrier, resolvedSlot, new ItemStack(Items.ELYTRA), ci);
            }
        }

        if (carrier != null) {
            var mod = resolveModification(carrier, resolvedSlot, stack);
            var itemInfo = mod.itemInfo() != null ? mod.itemInfo() : new ItemInfo(stack);
            if (itemInfo.isElytra() && carrier.isPlayerFlying()) {
                return RenderInterceptionResult.ignore();
            }
            return super.standardIntercept(carrier, resolvedSlot, stack, ci);
        }

        resolveModification(null, null, null);
        return RenderInterceptionResult.ignore();
    }
}
