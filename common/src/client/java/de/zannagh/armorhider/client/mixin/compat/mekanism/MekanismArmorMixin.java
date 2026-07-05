//? if mekanism {
/*package de.zannagh.armorhider.client.mixin.compat.mekanism;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.compat.mekanism.MekanismRenderCompat;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import mekanism.client.render.layer.MekanismArmorLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = MekanismArmorLayer.class, remap = false)
public abstract class MekanismArmorMixin <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends HumanoidArmorLayer<T, M, A> {

    public MekanismArmorMixin(RenderLayerParent<T, M> p_267286_, A p_267110_, A p_267150_, ModelManager p_267238_) {
        super(p_267286_, p_267110_, p_267150_, p_267238_);
    }

    @Inject(method = "renderArmorPart", at = @At("HEAD"), cancellable = true, require = 0)
    private void interceptMekanismArmor(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            T entity,
            EquipmentSlot slot,
            int light,
            float partialTicks,
            CallbackInfo ci) {
        if (!(entity instanceof IdentityCarrier carrier)) {
            return;
        }
        var ctx = AhRenderManagementApi.enterScope(RenderScope.ARMOR_PIECE, carrier, slot, entity.getItemBySlot(slot));
        var mod = ctx.modification();

        if (mod != null && mod.transparency() < 1.0) {
            HumanoidModel<T> parentModel = this.getParentModel();
            MekanismRenderCompat.renderBodyUnderArmor(parentModel, poseStack, bufferSource, entity, slot, light, partialTicks);
        }

        if (mod.shouldHide()) {
            AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
        }
    }

    @ModifyArg(
            method = "renderArmorPart",
            at = @At(value = "INVOKE",
                    target = "Lmekanism/client/render/armor/ICustomArmor;render(Lnet/minecraft/client/model/HumanoidModel;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIFZLnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"),
            index = 2,
            require = 0)
    private MultiBufferSource wrapBufferForTransparency(MultiBufferSource original) {
        var armorCtx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (!armorCtx.isEmpty() && armorCtx.modification().transparency() < 1.0) {
            return MekanismRenderCompat.wrapForTransparency(original);
        }
        return original;
    }

    @Inject(method = "renderArmorPart", at = @At("RETURN"), require = 0)
    private <T extends LivingEntity> void clearMekanismContext(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            T entity,
            EquipmentSlot slot,
            int light,
            float partialTicks,
            CallbackInfo ci) {
        AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
    }
}
*///?}
