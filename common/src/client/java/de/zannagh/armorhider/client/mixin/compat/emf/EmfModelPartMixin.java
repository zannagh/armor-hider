//? if >= 1.21 {
package de.zannagh.armorhider.client.mixin.compat.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.models.parts.EMFModelPart;

@Pseudo
@Mixin(value = EMFModelPart.class, remap = false)
public abstract class EmfModelPartMixin {

    private static final java.lang.reflect.Field VANILLA_ROOT_FIELD;

    static {
        java.lang.reflect.Field f = null;
        try {
            var rootClass = Class.forName("traben.entity_model_features.models.parts.EMFModelPartRoot");
            f = rootClass.getField("vanillaRoot");
            f.setAccessible(true);
        } catch (Exception ignored) {
        }
        VANILLA_ROOT_FIELD = f;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void armorHider$renderVanillaWhenForced(PoseStack matrices, VertexConsumer vertices, int light, int overlay, int k, CallbackInfo ci) {
        if (VANILLA_ROOT_FIELD == null) return;
        if (!EMFAnimationEntityContext.isEntityForcedToVanillaModel()) return;
        if (!VANILLA_ROOT_FIELD.getDeclaringClass().isInstance(this)) return;

        try {
            var vanilla = (ModelPart) VANILLA_ROOT_FIELD.get(this);
            if (vanilla != null) {
                vanilla.render(matrices, vertices, light, overlay, k);
                ci.cancel();
            }
        } catch (IllegalAccessException ignored) {
        }
    }
}
//?}

//? if < 1.21 {
/*package de.zannagh.armorhider.client.mixin.compat.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.models.parts.EMFModelPart;

@Pseudo
@Mixin(value = EMFModelPart.class, remap = false)
public abstract class EmfModelPartMixin {

    private static final java.lang.reflect.Field VANILLA_ROOT_FIELD;

    static {
        java.lang.reflect.Field f = null;
        try {
            var rootClass = Class.forName("traben.entity_model_features.models.parts.EMFModelPartRoot");
            f = rootClass.getField("vanillaRoot");
            f.setAccessible(true);
        } catch (Exception ignored) {
        }
        VANILLA_ROOT_FIELD = f;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void armorHider$renderVanillaWhenForced(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (VANILLA_ROOT_FIELD == null) return;
        if (!EMFAnimationEntityContext.isEntityForcedToVanillaModel()) return;
        if (!VANILLA_ROOT_FIELD.getDeclaringClass().isInstance(this)) return;

        try {
            var vanilla = (ModelPart) VANILLA_ROOT_FIELD.get(this);
            if (vanilla != null) {
                vanilla.render(matrices, vertices, light, overlay, red, green, blue, alpha);
                ci.cancel();
            }
        } catch (IllegalAccessException ignored) {
        }
    }
}
*///?}
