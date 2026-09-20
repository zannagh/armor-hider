package de.zannagh.armorhider.client.mixin.compat.fabricapi;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.compat.FabricArmorRendererCompat;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;

//? if >= 1.21.2
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

//? if < 1.21.9 {
/*import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

//? if < 1.21.2 {
/*import net.minecraft.world.entity.LivingEntity;
*///?}

/**
 * Compatibility mixin for Fabric API's {@code ArmorRenderer} (fabric-rendering-v1) dispatch.
 *
 * <p>Fabric API injects at the HEAD of {@code HumanoidArmorLayer.renderArmorPiece}: when the worn item has
 * a registered {@code ArmorRenderer} it draws the mod's own model and cancels the vanilla body. Armor
 * Hider's own {@code bodyKneesAndToes.HumanoidArmorLayerMixin} hooks the same method at the same (default)
 * mixin priority, and two same-priority HEAD injections from different mods have no guaranteed order -
 * whenever Fabric API's is applied later its cancel fires first and Armor Hider never sees the piece, so
 * mod armor drawn this way ignores every hide/opacity setting (issue #348).
 *
 * <p>Rather than racing, this mixin declares a <b>lowered</b> {@code priority}. Mixin applies a target's
 * mixins in ascending priority order and, at a HEAD injection point, the callbacks end up in application
 * order - so the lowest priority is applied first and its callback runs first, ahead of Fabric API's
 * dispatch at the default 1000. A fully hidden piece is then cancelled before anything draws.
 *
 * <p>The scope has to be <i>closed</i> somewhere Fabric API's cancel cannot skip, which rules out
 * {@code renderArmorPiece}'s own RETURN: {@code @At("RETURN")} binds to the returns present when the mixin
 * is applied, and the one {@code ci.cancel()} emits is not among them at any priority. So the bracket is
 * closed on the RETURN of the enclosing layer method - which nothing cancels - plus a backstop at the top
 * of the next piece. Both orderings were established by running {@code FabricArmorRendererSmokeTest},
 * which asserts the hide takes effect <i>and</i> that the armor-piece scope never leaks.
 *
 * <p>The whole hook is inert unless {@link FabricArmorRendererCompat#hasCustomRenderer(ItemStack)} says
 * Fabric API is about to take this piece over, so the vanilla armor path - and every other mod that hooks
 * {@code renderArmorPiece}, e.g. Armored Elytra's chestplate swap - keeps its existing ordering untouched.
 * Armor Hider's own layer mixin still runs afterwards and re-enters the same scope with the same
 * modification, which is a no-op on the single-entry scope map.
 *
 * <p>Geometry (opacity, render type) is handled by {@link FabricArmorRendererGeometryMixin} and
 * {@link FabricArmorRendererTypeMixin}, both gated on the drawing window opened here.
 */
@Mixin(value = HumanoidArmorLayer.class, priority = 500)
public class FabricArmorRendererLayerMixin
//? if < 1.21.2
//<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
{

    /**
     * The enclosing layer method - the one that calls {@code renderArmorPiece} once per slot. Spelled with
     * its full descriptor because from 1.21.2 the class also carries a synthetic bridge of the same name.
     */
    @Unique
    private static final String LAYER_ENTRY =
            //? if >= 1.21.9 {
            "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V";
            //? } elif >= 1.21.2 {
            /*"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V";
            *///? } else {
            /*"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V";*/
            //? }

    // ===== Render-state capture (1.21.2-1.21.8 only) =====
    // In this range renderArmorPiece receives the armor model instead of the render state, so the
    // identity-carrying state has to be stashed from the enclosing call - the same trick Armor Hider's own
    // HumanoidArmorLayerMixin uses, repeated here because a @Unique field of another mixin is not
    // reachable from this one.

    //? if >= 1.21.2 && < 1.21.9 {
    /*@Unique
    private static final ThreadLocal<Object> armorHider$fabricRenderState = new ThreadLocal<>();

    @Inject(method = LAYER_ENTRY, at = @At("HEAD"))
    private void armorHider$captureFabricRenderState(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, HumanoidRenderState renderState, float f, float g, CallbackInfo ci) {
        armorHider$fabricRenderState.set(renderState);
    }
    *///?}

    // ===== Scope bracket around Fabric API's dispatch =====

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    //? if >= 1.21.9
    private <S extends HumanoidRenderState> void armorHider$enterFabricArmor(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot slot, int packedLight, S humanoidRenderState, CallbackInfo ci) {
    //? if >= 1.21.2 && < 1.21.9
    //private void armorHider$enterFabricArmor(PoseStack poseStack, MultiBufferSource bufferSource, ItemStack itemStack, EquipmentSlot slot, int packedLight, HumanoidModel<?> armorModel, CallbackInfo ci) {
    //? if < 1.21.2
    //private void armorHider$enterFabricArmor(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, CallbackInfo ci) {

        //? if >= 1.21.9 {
        ItemStack wornStack = itemStack;
        Object identitySource = humanoidRenderState;
        //?} elif >= 1.21.2 {
        /*ItemStack wornStack = itemStack;
        Object identitySource = armorHider$fabricRenderState.get();
        *///?} else {
        /*ItemStack wornStack = entity.getItemBySlot(slot);
        Object identitySource = entity;
        *///?}

        // Backstop, before anything else: close a window the previous piece left open because Fabric API
        // cancelled out of it. The enclosing-method RETURN below is what guarantees a bracket never
        // survives the layer; closing here as well keeps one from ever spanning two pieces.
        armorHider$closeOpenBracket();

        if (!FabricArmorRendererCompat.hasCustomRenderer(wornStack)) {
            return;
        }
        if (!(identitySource instanceof IdentityCarrier carrier)) {
            return;
        }

        var ctx = AhRenderManagementApi.enterScope(RenderScope.ARMOR_PIECE, carrier, slot, wornStack);
        FabricArmorRendererCompat.recordBracketedPiece(ctx.shouldCancel());
        if (ctx.shouldCancel()) {
            // Nothing will draw, so close right away rather than leaving it to the layer RETURN - a hide
            // scope held open bleeds alpha 0 onto later submits.
            AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
            return;
        }
        FabricArmorRendererCompat.beginCustomArmorRender();
    }

    @Inject(method = LAYER_ENTRY, at = @At("RETURN"))
    private void armorHider$exitFabricArmorLayer(CallbackInfo ci) {
        //? if >= 1.21.2 && < 1.21.9
        //armorHider$fabricRenderState.remove();
        armorHider$closeOpenBracket();
    }

    @Unique
    private void armorHider$closeOpenBracket() {
        if (!FabricArmorRendererCompat.isDrawingCustomArmor()) {
            return;
        }
        FabricArmorRendererCompat.endCustomArmorRender();
        AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
    }
}
