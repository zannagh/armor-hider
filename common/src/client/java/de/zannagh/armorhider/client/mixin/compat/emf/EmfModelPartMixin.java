package de.zannagh.armorhider.client.mixin.compat.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.client.common.VanillaRootAccessor;
import de.zannagh.armorhider.log.DebugLogger;
import net.minecraft.client.model.geom.ModelPart;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.models.parts.EMFModelPart;

@Pseudo
@Mixin(value = EMFModelPart.class, remap = false)
public abstract class EmfModelPartMixin {

    @Unique
    @NonNull
    private ModelPart thisAsPart() { return (ModelPart) (Object) this; }
    
    @Unique
    private static int armorHider$logCounter = 0;

    @Unique
    private static final java.util.Set<Integer> armorHider$renderedVanillaRoots = new java.util.HashSet<>();

    @Unique
    private static long armorHider$lastFrameTime = 0;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    //? if >= 1.21 {
    private void armorHider$renderVanillaWhenForced(PoseStack matrices, VertexConsumer vertices, int light, int overlay, int k, CallbackInfo ci) {
    //? } else
    // private void armorHider$renderVanillaWhenForced(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {

        @SuppressWarnings("deprecation")
        boolean emfForced = EMFAnimationEntityContext.isEntityForcedToVanillaModel();
        boolean playerForced = AhRenderManagementApi.shouldEnforceVanillaRendering();
        if (!emfForced && !playerForced) {
            return;
        }

        if (this instanceof VanillaRootAccessor accessor) {
            ModelPart vanilla = accessor.armorHider$getVanillaRoot();
            if (vanilla != null) {
                long now = System.nanoTime() / 1_000_000;
                if (now != armorHider$lastFrameTime) {
                    armorHider$renderedVanillaRoots.clear();
                    armorHider$lastFrameTime = now;
                }
                int id = System.identityHashCode(vanilla);
                if (armorHider$renderedVanillaRoots.add(id)) {
                    if (DebugLogger.isEnabled() && armorHider$logCounter++ % 600 == 0) {
                        DebugLogger.log("[EMF mixin] REDIRECT to vanillaRoot | id={} | player={} | class={}", id, AhRenderManagementApi.currentlyHandledPlayerName(), this.getClass().getSimpleName());
                    }
                    RenderModifications.synchronisePoses(thisAsPart(), vanilla);
                    
                    //? if >= 1.21 {
                    vanilla.render(matrices, vertices, light, overlay, k);
                    //? } else
                    // vanilla.render(matrices, vertices, light, overlay, red, green, blue, alpha);
                } else if (DebugLogger.isEnabled() && armorHider$logCounter++ % 600 == 0) {
                    DebugLogger.log("[EMF mixin] DEDUP vanillaRoot | id={} | player={} | class={}", id, AhRenderManagementApi.currentlyHandledPlayerName(), this.getClass().getSimpleName());
                }
                ci.cancel();
                return;
            }
        }

        if (DebugLogger.isEnabled() && armorHider$logCounter++ % 600 == 0) {
            DebugLogger.log("[EMF mixin] CANCEL (non-root) | player={} | class={}", AhRenderManagementApi.currentlyHandledPlayerName(), this.getClass().getSimpleName());
        }
        ci.cancel();
    }
}
