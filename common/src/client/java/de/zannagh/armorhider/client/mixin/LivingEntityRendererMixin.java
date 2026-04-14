package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.client.scopes.IdentityStateCarrier;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.4 {
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//?}

//? if < 1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

//? if < 1.21.4 {
/*import net.minecraft.client.model.HumanoidModel;
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
public abstract class LivingEntityRendererMixin
    //? if >= 1.21.4 {
        <T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> implements RenderLayerParent<S, M>
    //?}
    //? if < 1.21.4 {
    //<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M>
    //?}
    {

    @Shadow
    protected M model;

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    //? if >= 1.21.4 {
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
    //?}

    //? if >= 1.21.4 && < 1.21.9 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void forceArmVisibility(LivingEntityRenderState state, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        forceArmVisibilityFromState(state);
    }

    @Unique
    private void forceArmVisibilityFromState(EntityRenderState state) {
        if (!ArmorHiderClient.GECKOLIB_LOADED || !ArmorHiderClient.FA_LOADED) {
            return;
        }
        
        if (!(state instanceof IdentityCarrier carrier)) {
            return;
        }
        if (!(state instanceof HumanoidRenderState humanoidState)) {
            return;
        }

        String name = carrier.playerName();
        if (name == null) { 
            return;
        }

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

    //? if < 1.21.4 {
    /*@Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void forceArmVisibility(LivingEntity entity, float yBodyRot, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (!ArmorHiderClient.GECKOLIB_LOADED || !ArmorHiderClient.FA_LOADED) {
            return;
        }
        if (!(entity instanceof IdentityCarrier carrier)) {
            return;
        }

        String name = carrier.playerName();
        if (name == null) {
            return;
        }

        if (ActiveModification.isSlotModified(name, EquipmentSlot.CHEST, entity.getItemBySlot(EquipmentSlot.CHEST))) {
            var model = ((LivingEntityRenderer<?, ?>) (Object) this).getModel();
            if (model instanceof HumanoidModel<?> humanoid) {
                humanoid.leftArm.visible = true;
                humanoid.rightArm.visible = true;
            }
        }
    }
    *///?}
}
