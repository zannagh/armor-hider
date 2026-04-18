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
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin implements IdentityCarrier {

    @Unique
    private boolean armorHider$needsArmRerender;

    @Override
    public void setNeedsArmRerender() {
        armorHider$needsArmRerender = true;
    }

    @Override
    public boolean pollNeedsArmRerender() {
        boolean needs = armorHider$needsArmRerender;
        armorHider$needsArmRerender = false;
        return needs;
    }
    
    @Override
    public @Nullable String armorHider$playerName() {
        return PlayerNameUtil.getPlayerName(this);
    }

    @Override
    public @Nullable ItemStack customHeadItem() {
        Player player = (Player) (Object) this;
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.isEmpty() ? null : head;
    }

    @Override
    public boolean isPlayerFlying() {
        Player player = (Player) (Object) this;
        return player.isFallFlying() || player.getAbilities().flying;
    }

    @ModifyReturnValue(method = "getItemBySlot", at = @At("RETURN"))
    private ItemStack hideFullyHiddenSlot(ItemStack original, EquipmentSlot slot) {
        if (original.isEmpty()) {
            return original;
        }

        var ctx = ArmorHiderClient.RENDER_CONTEXT;

        if (ctx.hasActiveModification()) {
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

        if (ActiveModification.isSlotFullyHidden(armorHider$playerName(), slot, original)) {
            DebugTracer.equipmentSlotHidingFired(armorHider$playerName(), slot, true, "isSlotFullyHidden");
            return ItemStack.EMPTY;
        }
        return original;
    }
}
