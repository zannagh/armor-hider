package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SimpleEquipmentLayer.class)
public abstract class EquipmentLayerMixin<S extends LivingEntityRenderState, RM extends EntityModel<? super S>, EM extends EntityModel<? super S>>
        extends RenderLayer<S, RM> {
    public EquipmentLayerMixin(RenderLayerParent<S, RM> renderLayerParent) {
        super(renderLayerParent);
    }
}
