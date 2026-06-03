package de.zannagh.armorhider.client.rendering.interceptors;

import com.ibm.icu.text.Normalizer2;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.render.RenderInterceptor;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class EquipmentLayerInterceptor<S>
    extends RenderInterceptor<EquipmentLayerRenderer, S, Model<? super S>>
{
    @Override
    public boolean shouldIntercept(EquipmentLayerRenderer context, @Nullable IdentityCarrier carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack itemStack, @Nullable S entity, @Nullable Model<? super S> model){
        super.shouldIntercept(context, carrier, slot, itemStack, entity, model);

        return true;
    }

    @Override
    public boolean shouldCancelRender(EquipmentLayerRenderer context) {
        return false;
    }

    @Override
    public void interceptRender(EquipmentLayerRenderer context, CallbackInfo ci) {

    }
}
