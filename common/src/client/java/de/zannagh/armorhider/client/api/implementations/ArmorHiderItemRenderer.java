package de.zannagh.armorhider.client.api.implementations;

import de.zannagh.armorhider.client.api.ArmorHiderRenderModificationApi;
import de.zannagh.armorhider.client.api.ArmorHiderRenderer;
import de.zannagh.armorhider.client.common.*;
import de.zannagh.armorhider.client.rendering.VanillaArmorTextureManager;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ArmorHiderItemRenderer implements ArmorHiderRenderer {

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.ARMOR_PIECE;
    }

    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        SlotModification modification;
        if (identityCarrier instanceof IdentityCarrier identityCarrier1) {
            modification = SlotModification.of(identityCarrier1.armorHider$playerName(), slot, stack);
        }
        else {
            modification = SlotModification.empty();
        }

        if (modification.isEmpty()) {
            return RenderInterceptionResult.ignore();
        }

        if (modification.shouldHide()) {
            return new RenderInterceptionResult(true, true);
        }
        return new RenderInterceptionResult(true, false);
    }

    @Override
    public ArmorHiderRenderModificationApi getRenderModificationApi() {
        return null;
    }
}
