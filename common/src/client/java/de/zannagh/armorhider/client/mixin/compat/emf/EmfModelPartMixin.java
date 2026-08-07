package de.zannagh.armorhider.client.mixin.compat.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.render.AhArmProbe;
import de.zannagh.armorhider.client.render.EmfHiddenModeContext;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.configuration.EmfHiddenModelMode;
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

    // #217 "Vanilla on seam areas": the vanilla player parts we iterate at the root so the torso and
    // arms can fall back to vanilla while the head and legs keep the custom (Fresh Animations) model.
    @Unique
    private static final String[] armorHider$playerParts =
            {"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"};

    @Unique
    private static final java.util.Set<String> armorHider$seamParts =
            java.util.Set.of("body", "left_arm", "right_arm");

    // Re-entry guard so the per-part renders we trigger below aren't intercepted again by this mixin.
    @Unique
    private static boolean armorHider$compositing = false;

    @Unique
    @NonNull
    private ModelPart thisAsPart() { return (ModelPart) (Object) this; }

    /**
     * "Vanilla on seam areas" render: draw the torso and arms from the vanilla model (carrying the
     * live pose) and the remaining parts from the custom model, so the arm/torso seam is closed while
     * the head and legs keep Fresh Animations.
     */
    @Unique
    //? if >= 1.21 {
    private static void armorHider$renderSeamComposite(ModelPart faRoot, ModelPart vanillaRoot, PoseStack matrices, VertexConsumer vertices, int light, int overlay, int k) {
    //? } else
    // private static void armorHider$renderSeamComposite(ModelPart faRoot, ModelPart vanillaRoot, PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        RenderModifications.synchronisePoses(faRoot, vanillaRoot);
        if (AhArmProbe.isEnabled()) {
            AhArmProbe.recordSeamComposite();
        }
        armorHider$compositing = true;
        try {
            for (String part : armorHider$playerParts) {
                ModelPart source = armorHider$seamParts.contains(part) ? vanillaRoot : faRoot;
                if (source.hasChild(part)) {
                    //? if >= 1.21 {
                    source.getChild(part).render(matrices, vertices, light, overlay, k);
                    //? } else
                    // source.getChild(part).render(matrices, vertices, light, overlay, red, green, blue, alpha);
                }
            }
        } finally {
            armorHider$compositing = false;
        }
    }

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

        // Pass through the per-part renders we trigger during a seam composite (below).
        if (armorHider$compositing) {
            return;
        }

        @SuppressWarnings("deprecation")
        boolean emfForced = EMFAnimationEntityContext.isEntityForcedToVanillaModel();
        boolean playerForced = AhRenderManagementApi.shouldEnforceVanillaRendering();
        if (!emfForced && !playerForced) {
            // Not forced: EMF draws its own (possibly custom) model. Record it so the #217 smoke
            // test can see that the un-fixed path leaves the custom model (with its seam) in place.
            if (AhArmProbe.isEnabled() && this instanceof VanillaRootAccessor) {
                AhArmProbe.recordCustomModel();
            }

            // #217 "Vanilla on seam areas": mix vanilla torso/arms with the custom head/legs at the
            // root. EMF publishes the player's mode via EmfHiddenModeContext just before rendering.
            if (EmfHiddenModeContext.current() == EmfHiddenModelMode.VANILLA_SEAMS
                    && this instanceof VanillaRootAccessor accessor) {
                ModelPart vanillaRoot = accessor.armorHider$getVanillaRoot();
                if (vanillaRoot != null) {
                    //? if >= 1.21 {
                    armorHider$renderSeamComposite(thisAsPart(), vanillaRoot, matrices, vertices, light, overlay, k);
                    //? } else
                    // armorHider$renderSeamComposite(thisAsPart(), vanillaRoot, matrices, vertices, light, overlay, red, green, blue, alpha);
                    ci.cancel();
                    return;
                }
            }
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

                    // Forced to vanilla: the clean vanilla model is what gets drawn. Record it so the
                    // #217 smoke test can assert that hiding the body closed the custom-model seam.
                    if (AhArmProbe.isEnabled()) {
                        AhArmProbe.recordForcedVanilla();
                    }

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
