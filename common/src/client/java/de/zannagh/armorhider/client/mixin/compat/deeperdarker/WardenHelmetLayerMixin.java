//? if deeperdarker_warden {
/*package de.zannagh.armorhider.client.mixin.compat.deeperdarker;

import com.kyanite.deeperdarker.client.render.WardenHelmetRenderer;
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
 * Compatibility mixin for Deeper and Darker's Warden armor helmet on the mod's 1.4.x builds
 * (which ship the renderer as {@code WardenHelmetRenderer}; the 1.3.x builds call it
 * {@code HelmetHornRenderer} — see {@code HelmetHornLayerMixin}).
 * <p>
 * The Warden armor pieces are plain {@code ArmorItem}s, so their body renders through the vanilla
 * armor layer and is already hidden by armor-hider's {@code HumanoidArmorLayer}/{@code renderArmorPiece}
 * cancellation. The helmet's horns, however, are drawn by this separate {@code RenderLayer} added to
 * player renderers, which bypasses that path — leaving the horns visible ("sticking out") when the rest
 * of the helmet is hidden.
 * <p>
 * This coarse hide-cancellation drops the horn layer whenever the wearer's HEAD slot is configured to
 * hide, so the horns disappear together with the helmet. Resolving through
 * {@link IdentityCarrier#getModification} keeps per-item exclusions and the opacity threshold intact.
 * <p>
 * Entity-based {@code RenderLayer#render} signature — only valid on MC &lt; 1.21.2. Deeper and Darker
 * publishes no build above 1.21.1, so every target that exists is entity-based and no render-state
 * ({@code >= 1.21.2}) variant is needed. Gated by the {@code deeperdarker_warden} constant, set for
 * variants pinning a D&amp;D 1.4.x jar (currently NeoForge 1.21.1) via {@code deeperdarker.warden_class}.
 ^/
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(value = WardenHelmetRenderer.class, remap = false)
public class WardenHelmetLayerMixin {

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
