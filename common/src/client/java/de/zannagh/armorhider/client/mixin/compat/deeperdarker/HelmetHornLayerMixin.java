//? if deeperdarker_horn {
/*package de.zannagh.armorhider.client.mixin.compat.deeperdarker;

import com.kyanite.deeperdarker.client.render.HelmetHornRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
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
 * Compatibility mixin for Deeper and Darker's Warden armor helmet on the mod's 1.3.x builds
 * (which ship the renderer as {@code HelmetHornRenderer}; the 1.4.x builds renamed it
 * {@code WardenHelmetRenderer} — see {@code WardenHelmetLayerMixin}).
 * <p>
 * Identical behaviour to {@code WardenHelmetLayerMixin}: the Warden armor body renders via the vanilla
 * armor layer (already hidden by our {@code renderArmorPiece} cancellation), while the helmet horns are
 * this separate {@code RenderLayer} on player renderers. Cancel it when the wearer's HEAD slot is
 * configured to hide, so the horns disappear together with the helmet. Resolving through
 * {@link IdentityCarrier#getModification} keeps per-item exclusions and the opacity threshold intact.
 * <p>
 * Entity-based {@code RenderLayer#render} signature (MC &lt; 1.21.2). Gated by the
 * {@code deeperdarker_horn} constant — set for {@code deeperdarker}-enabled variants that do NOT pin a
 * 1.4.x jar (currently Fabric 1.20.1 and Fabric 1.21.1, both D&amp;D 1.3.3).
 ^/
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(value = HelmetHornRenderer.class, remap = false)
public class HelmetHornLayerMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void armorHider$hideWardenHelmetHorns(PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, LivingEntity entity,
            float limbAngle, float limbDistance,
            float partialTicks, float animationProgress, float headYaw, float headPitch,
            CallbackInfo ci) {
        if (!(entity instanceof IdentityCarrier carrier)) {
            return;
        }
        ItemStack headItem = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (carrier.getModification(EquipmentSlot.HEAD, headItem).shouldHide()) {
            ci.cancel();
        }
    }
}
*///?}
