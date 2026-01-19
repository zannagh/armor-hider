// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class ArmorFeatureRenderMixin {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private <S extends HumanoidRenderState> void captureContext(
            PoseStack poseStack, SubmitNodeCollector submitNodeCollector, net.minecraft.world.item.ItemStack itemStack, net.minecraft.world.entity.EquipmentSlot equipmentSlot, int i, S humanoidRenderState, CallbackInfo ci) {
        ArmorRenderPipeline.setupContext(itemStack, equipmentSlot, humanoidRenderState);
    }
}

