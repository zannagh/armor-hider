package de.zannagh.armorhider.client.mixin.compat.emf;

import de.zannagh.armorhider.client.common.VanillaRootAccessor;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import traben.entity_model_features.models.parts.EMFModelPartRoot;

@Pseudo
@Mixin(value = EMFModelPartRoot.class, remap = false)
public abstract class EmfModelPartRootMixin implements VanillaRootAccessor {

    @Final
    @Shadow
    public ModelPart vanillaRoot;

    @Override
    public ModelPart armorHider$getVanillaRoot() {
        return vanillaRoot;
    }
}
