// | --------------------------------------------------- |
// | This mechanic is inspired by Show Me Your Skin!     |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.combat.ClientCombatManager;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.log.DebugTracer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements IdentityCarrier {
    
    @Unique
    private Player getPlayer() {
        return (Object) this instanceof Player playerInstance ? playerInstance : null;
    }
    
    @Inject(
            method = "handleDamageEvent",
            at = @At(value = "HEAD")
    )
    private void triggerCombat(DamageSource damageSource, CallbackInfo ci) {
        ClientCombatManager.handleCombat(ClientCombatManager::shouldLogCombatForPlayer, damageSource, getPlayer());
    }

    @Override
    public @Nullable String armorHider$getPlayerName() {
        return getPlayer() == null ? null : getPlayer().getName().getString();
    }

    @Override
    public @Nullable ItemStack armorHider$customHeadItem() {
        if (getPlayer() != null) {
            if (!getPlayer().getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                return getPlayer().getItemBySlot(EquipmentSlot.HEAD);
            }
        }
        return null;
    }


    @Override
    public boolean armorHider$isPlayerFlying() {
        return getPlayer() != null && (getPlayer().isFallFlying() || getPlayer().getAbilities().flying);
    }

    /** Sets the equipment slot's item stack to non-empty / original if the game is during tick processing or GUI rendering, otherwise sets the slots according to render interceptions. */
    @ModifyReturnValue(method = "getItemBySlot", at = @At("RETURN"))
    private ItemStack hideFullyHiddenSlot(ItemStack original, EquipmentSlot slot) {
        if (original.isEmpty()) {
            return original;
        }

        var ctx = ArmorHiderClient.RENDER_CONTEXT;

        if (ctx.hasActiveModification()) {
            // Let armor-hider handle rendering.
            return original;
        }

        // Only fake empty slots during level rendering (3D world) — never during
        // game logic (tick processing, inventory interactions) or HUD/GUI rendering.
        if (!ctx.isInLevelRender()) {
            return original;
        }

        // During entity rendering (extractRenderState + layer rendering), return the
        // real item so that renderArmorPiece is called (for downstream render processing).
        if (ctx.isInEntityRender()) {
            return original;
        }

        if (getPlayer() == null) {
            return original;
        }
        String playerName = getPlayer().getName().getString();

        if (ActiveModification.isSlotFullyHidden(playerName, slot, original)) {
            DebugTracer.equipmentSlotHidingFired(playerName, slot, true, "isSlotFullyHidden");
            return ItemStack.EMPTY;
        }
        return original;
    }
}
