package de.zannagh.armorhider.client.api.implementations;

import de.zannagh.armorhider.client.api.ArmorHiderRenderModificationApi;
import de.zannagh.armorhider.client.api.ArmorHiderRenderer;
import de.zannagh.armorhider.client.common.*;
import de.zannagh.armorhider.client.rendering.VanillaArmorTextureManager;
import de.zannagh.armorhider.common.ItemInfo;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ArmorHiderItemRenderer implements ArmorHiderRenderer {

    private final ThreadLocal<ArmorHiderRenderModificationApi> modificationApi = new ThreadLocal<>();

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.ARMOR_PIECE;
    }

    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        SlotModification modification;
        ItemInfo itemInfo = new ItemInfo(stack);
        IdentityCarrier carrier = null;
        if (slot == null && stack != null) {
            slot = itemInfo.getEquippableSlot();
        }

        if (identityCarrier instanceof IdentityCarrier identityCarrier1) {
            carrier = identityCarrier1;
            modification = SlotModification.of(identityCarrier1.armorHider$playerName(), slot, stack);
            if (itemInfo.isEmpty() && slot != null) {
                itemInfo = modification.itemInfo();
            }
            if (itemInfo.isElytra() && identityCarrier1.isPlayerFlying()) {
                modificationApi.set(new RenderModifications(modification));
                return RenderInterceptionResult.ignore();
            }
        }
        else {
            modification = SlotModification.empty();
        }

        modificationApi.set(new RenderModifications(modification));
        if (modification.isEmpty()) {
            return RenderInterceptionResult.ignore();
        }

        if (modification.shouldHide()) {
            if (ci.isCancellable() && !ci.isCancelled()) {
                ci.cancel();
            }
            return new RenderInterceptionResult(true, true, getTargetScope(), carrier, modification);
        }
        return new RenderInterceptionResult(true, false, getTargetScope(), carrier, modification);
    }

    @Override
    public ArmorHiderRenderModificationApi getRenderModificationApi() {
        return modificationApi.get();
    }
}
