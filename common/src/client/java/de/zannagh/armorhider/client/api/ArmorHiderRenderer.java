package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScopeProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface ArmorHiderRenderer extends RenderScopeProvider {

    RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci);

    /**
     * Gets the render modification API for this renderer based on the last interception if it is not available on the scope currently or has to be used immediately.
     * @return The render modification API.
     */
    ArmorHiderRenderModificationApi getRenderModificationApi();
}
