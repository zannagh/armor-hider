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

        // Only fake empty slots during the render frame — never during game logic
        // (tick processing, inventory interactions), as returning empty there causes
        // items to vanish during equipment swaps (e.g. right-clicking elytra to swap
        // with a hidden chestplate).
        if (!ArmorRenderPipeline.isInRenderFrame()) {
            return original;
        }

        // During entity rendering (extractRenderState + layer rendering), return the
        // real item so that renderArmorPiece is called. This allows our cancel at
        // renderArmorPiece HEAD to fire, which in turn lets mods like Essential detect
        // render suppression and show cosmetics/skins.
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
