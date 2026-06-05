package de.zannagh.armorhider.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
//?}

//? if >= 26.1-0.snapshot.11
import net.minecraft.client.renderer.state.level.CameraRenderState;

//? if >= 1.21.9 && < 26.1-0.snapshot.11
//import net.minecraft.client.renderer.state.level.CameraRenderState;

//? if < 1.21.9 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
*///?}

// Sets a flag around the entire entity render cycle (extractRenderState + layer rendering)
// so that PlayerMixin's slot hiding returns real items during rendering. This ensures
// renderArmorPiece is called for hidden armor, allowing mods like Essential to detect
// render suppression at that level.
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Unique
    //? if >= 1.21.9
    private static final String RENDER_METHOD = "submit";
    //? if >= 1.21.4 && < 1.21.9
    //private static final String RENDER_METHOD = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    //? if < 1.21.4
    //private static final String RENDER_METHOD = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Unique
    private static boolean isPlayerEntity(Object entity) {
        //? if >= 1.21.9
        return entity instanceof AvatarRenderState;
        //? if >= 1.21.4 && < 1.21.9
        //return entity instanceof Player;
        //? if < 1.21.4
        //return entity instanceof Player;
    }

    @Inject(method = RENDER_METHOD, at = @At("HEAD"))
    //? if >= 1.21.9
    private <S extends EntityRenderState> void enterEntityRendering(S entity, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
    //? if < 1.21.9 && >= 1.21.4
    //private <E extends Entity> void enterEntityRendering(E entity, double x, double y, double z, float yRot, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
    //? if < 1.21.4
    //private <E extends Entity> void enterEntityRendering(E entity, double x, double y, double z, float yRot, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (isPlayerEntity(entity)) {
            ArmorHiderClientApi.getInstance().getRenderingScopeApi().setInEntityRender();
            if (entity instanceof IdentityCarrier carrier) {
                ArmorHiderClientApi.getInstance().getRenderingScopeApi().setCurrentPlayer(carrier.armorHider$playerName());
            }
        }
    }

    @Inject(method = RENDER_METHOD, at = @At("RETURN"))
    //? if >= 1.21.9 {
    private <S extends EntityRenderState> void exitEntityRendering(S entity, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
    //?} else if >= 1.21.4 {
    /*private <E extends Entity> void exitEntityRendering(E entity, double x, double y, double z, float yRot, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
    *///?} else {
    /*private <E extends Entity> void exitEntityRendering(E entity, double x, double y, double z, float yRot, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
    *///?}
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitEntityRender();
    }
}
