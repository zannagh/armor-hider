//? if >= 1.21.9 {
package de.zannagh.armorhider.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Sets a flag around the entire entity render cycle (extractRenderState + layer rendering)
// so that EquipmentSlotHidingMixin can return real items during rendering. This ensures
// renderArmorPiece is called for hidden armor, allowing mods like Essential to detect
// render suppression at that level.
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(
            method = "submit",
            at = @At("HEAD")
    )
    private <S extends EntityRenderState> void enterEntityRendering(S entityRenderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        ArmorRenderPipeline.enterEntityRendering();
    }

    @Inject(
            method = "submit",
            at = @At("RETURN")
    )
    private <S extends EntityRenderState> void exitEntityRendering(S entityRenderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        ArmorRenderPipeline.exitEntityRendering();
    }
}
//?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Sets a flag around the entire entity render cycle (extractRenderState + layer rendering)
// so that EquipmentSlotHidingMixin can return real items during rendering. This ensures
// renderArmorPiece is called for hidden armor, allowing mods like Essential to detect
// render suppression at that level.
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private <E extends Entity> void enterEntityRendering(E entity, double x, double y, double z, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        ArmorRenderPipeline.enterEntityRendering();
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private <E extends Entity> void exitEntityRendering(E entity, double x, double y, double z, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        ArmorRenderPipeline.exitEntityRendering();
    }
}
*///?}
