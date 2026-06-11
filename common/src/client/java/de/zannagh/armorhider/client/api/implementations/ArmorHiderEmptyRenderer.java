package de.zannagh.armorhider.client.api.implementations;

import de.zannagh.armorhider.client.api.ArmorHiderRenderModificationApi;
import de.zannagh.armorhider.client.api.ArmorHiderRenderer;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ArmorHiderEmptyRenderer implements ArmorHiderRenderer {
    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        return RenderInterceptionResult.ignore();
    }

    @Override
    public ArmorHiderRenderModificationApi getRenderModificationApi() {
        return null;
    }

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.ALL;
    }
}
