//? if >= 1.21.4 {
package de.zannagh.armorhider.client.mixin;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.mixin.hand.OffHandRenderMixin;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.client.scopes.IdentityStateCarrier;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
//?if >= 1.21.11
import net.minecraft.client.model.player.PlayerModel;
//?if < 1.21.11
//import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
//?}
//? if >= 26.1-0.snapshot.11
import net.minecraft.client.renderer.state.level.CameraRenderState;

import java.lang.reflect.Method;
import java.util.List;
//? if >= 1.21.9 && < 26.1-0.snapshot.11
//import net.minecraft.client.renderer.state.CameraRenderState;
//? if >= 1.21.4 && < 1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

/**
 * Captures the player's identity from the actual entity during {@code extractRenderState},
 * before the entity reference is lost and only the render state remains.
 * <p>
 * Also counteracts compat mods (e.g. FantasyArmor) that hide the player model's arms
 * when GeckoLib-based armor is equipped, by forcing arm visibility at the start of
 * the render/submit call.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> implements RenderLayerParent<S, M> {
    
    @Shadow
    protected M model;

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * Enters the entity render scope during {@code extractRenderState} so that
     * {@code PlayerMixin}'s slot hiding does not
     * intercept {@code getItemBySlot} calls made by vanilla during state extraction.
     * <p>
     * In 1.21.4–1.21.8 this is redundant (the scope is already entered by
     * {@code EntityRenderDispatcherMixin} which hooks the wrapping {@code render(Entity)} method),
     * but harmless — {@code enterEntityRender()} simply resets to SENTINEL.
     * In 1.21.9+ it is required because {@code submit(EntityRenderState)} is a separate call.
     */
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("HEAD")
    )
    private void enterEntityRenderDuringExtraction(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState)) {
            return;
        }
        ArmorHiderClient.RENDER_CONTEXT.enterEntityRender();
    }

    /**
     * Captures the player's identity onto the render state object itself when rendering player entities.
     * For non-player entities, the render state's player name is left unchanged (each renderer owns its
     * own state object, so this is safe).
     */
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void capturePlayerIdentity(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (entity instanceof IdentityCarrier carrier 
                && state instanceof IdentityStateCarrier stateCarrier
                && state instanceof AvatarRenderState) { // Make sure we don't accidentally capture zombies or other humanoids.
            stateCarrier.attachCarrier(carrier);
        }
    }

    //? if >= 1.21.4 && < 1.21.9 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void forceArmVisibility(LivingEntityRenderState state, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        forceArmVisibilityFromState(state);
    }
    
    @Unique
    private void forceArmVisibilityFromState(EntityRenderState state) {
        if (!ArmorHiderClient.GECKOLIB_LOADED) return;
        if (!(state instanceof IdentityCarrier carrier)) return;
        if (!(state instanceof HumanoidRenderState humanoidState)) return;

        String name = carrier.playerName();
        if (name == null) return;

        if (ActiveModification.isSlotModified(name, EquipmentSlot.CHEST, humanoidState.chestEquipment)) {
            var model = ((LivingEntityRenderer<?, ?, ?>) (Object) this).getModel();
            if (model instanceof PlayerModel humanoid) {
                humanoid.leftArm.visible = true;
                humanoid.leftSleeve.visible = true;
                humanoid.rightArm.visible = true;
                humanoid.rightSleeve.visible = true;
            }
        }
    }
    *///?}

    
}
//?}

//? if < 1.21.4 {
/*package de.zannagh.armorhider.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Counteracts compat mods (e.g. FantasyArmor) that hide the player model's arms
 * when GeckoLib-based armor is equipped on the chest slot.
 * <p>
 * Those mods inject into {@code PlayerRenderer.render()} before the super call
 * and set the arm ModelParts invisible. By injecting at HEAD of
 * {@code LivingEntityRenderer.render()} (inside the super call), we run after
 * the compat mod and can restore arm visibility when our mod fully hides the
 * armor piece — so the player looks like they have no armor on, arms and all.
 ^/
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void forceArmVisibility(LivingEntity entity, float yBodyRot, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (!ArmorHiderClient.GECKOLIB_LOADED) return;
        if (!(entity instanceof IdentityCarrier carrier)) return;

        String name = carrier.playerName();
        if (name == null) return;

        if (ActiveModification.isSlotModified(name, EquipmentSlot.CHEST, entity.getItemBySlot(EquipmentSlot.CHEST))) {
            var model = ((LivingEntityRenderer<?, ?>) (Object) this).getModel();
            if (model instanceof HumanoidModel<?> humanoid) {
                humanoid.leftArm.visible = true;
                humanoid.rightArm.visible = true;
            }
        }
    }
}
*///?}
