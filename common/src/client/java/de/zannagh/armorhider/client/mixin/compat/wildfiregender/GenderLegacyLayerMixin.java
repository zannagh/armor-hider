//? if gender_legacy {
/*package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wildfire.render.GenderLayer;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Compatibility mixin for the legacy female-gender API.
 * <p>
 * The mod's NeoForge 1.21/1.21.1 build (modrinth hash {@code kKffHCGl}) ships only
 * {@link GenderLayer} with a monolithic {@code render(PoseStack, MultiBufferSource, int, ENTITY, float...)}
 * entrypoint and no separate {@code renderBreastArmor} / {@code renderArmorTrim}
 * hooks, so all we can do here is coarse hide-cancellation of the whole breast
 * geometry when the player's chest slot is configured to hide. Transparency and
 * armor-trim recolor are not supported on this combo.
 ^/
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(value = GenderLayer.class, remap = false)
public class GenderLegacyLayerMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void interceptRender(PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, LivingEntity entity,
            float limbAngle, float limbDistance,
            float partialTicks, float animationProgress, float headYaw, float headPitch,
            CallbackInfo ci) {
        if (!(entity instanceof IdentityCarrier carrier)) return;
        ItemStack chestItem = entity.getItemBySlot(EquipmentSlot.CHEST);
        var mod = carrier.ah$getModification(EquipmentSlot.CHEST, chestItem);
        if (mod.shouldHide()) {
            ci.cancel();
        }
    }
}
*///?}
