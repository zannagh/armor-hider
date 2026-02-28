package de.zannagh.armorhider.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


/**
 * Makes fully hidden equipment slots appear empty to other mods.
 * This allows mods like Essential to render custom skins or cosmetics
 * when Armor Hider has hidden the armor in that slot.
 * <p>
 * Only returns empty when Armor Hider's own rendering pipeline is NOT active,
 * so vanilla armor rendering (which armor-hider manages via its own cancel/transparency
 * mechanism) is unaffected.
 */
@Mixin(LivingEntity.class)
public class EquipmentSlotHidingMixin {
    
    @ModifyReturnValue(method = "getItemBySlot", at = @At("RETURN"))
    private ItemStack hideFullyHiddenSlot(ItemStack original, EquipmentSlot slot) {
        if (original.isEmpty()) {
            return original;
        }
        
        if (ArmorRenderPipeline.hasActiveContextOnAnySlot()) {
            // Let armor-hider handle rendering.
            return original;
        }
        
        // During entity rendering (extractRenderState + layer rendering), return the
        // real item so that renderArmorPiece is called. This allows our cancel at
        // renderArmorPiece HEAD to fire, which in turn lets mods like Essential detect
        // render suppression and show cosmetics/skins.
        
        // TODO: I'll have to document the high-level flow of the render interception at some point, it's now getting confusing..
        if (ArmorRenderPipeline.isInEntityRendering()) {
            return original;
        }
        
        //noinspection ConstantValue
        if (!((Object) this instanceof Player player)) {
            return original;
        }
        String playerName = player.getName().getString();
        
        if (ArmorRenderPipeline.isSlotFullyHidden(playerName, slot, original)) {
            return ItemStack.EMPTY;
        }
        return original;
    }
}
