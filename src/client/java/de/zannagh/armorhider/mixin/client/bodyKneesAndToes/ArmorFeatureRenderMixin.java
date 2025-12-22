// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureRenderMixin {
    @Inject(method = "renderArmor", at = @At("HEAD"))
    private void captureSlotAndCheckHide(
            MatrixStack matrices,
            OrderedRenderCommandQueue vertexConsumers,
            ItemStack stack,
            EquipmentSlot slot,
            int light,
            BipedEntityRenderState armorModel,
            CallbackInfo ci) {
        ArmorRenderPipeline.setCurrentSlot(slot);
    }
}