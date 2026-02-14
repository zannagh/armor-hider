//? if >= 1.21.11 {

package de.zannagh.armorhider.mixin.client.hand;

import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemRenderMixin {
    
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("HEAD"))
    private static void triggerRender(ItemEntity itemEntity, ItemEntityRenderState itemEntityRenderState, float f, CallbackInfo ci) {
        if (itemEntity.getOwner() instanceof Player player) {
            var slot = player.getEquipmentSlotForItem(itemEntity.getItem());
            if (slot != EquipmentSlot.OFFHAND) {
                return;
            }
            ArmorRenderPipeline.setupContext(itemEntity.getItem(), EquipmentSlot.OFFHAND, player.getGameProfile());
        }
    }
}

//?}