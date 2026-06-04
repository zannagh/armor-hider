// | --------------------------------------------------- |
// | This mechanic is inspired by Show Me Your Skin!     |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;import de.zannagh.armorhider.client.api.configuration.PlayerModificationInfo;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.log.DebugTracer;
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

@Mixin(Player.class)
public abstract class PlayerMixin 
    //? if >= 1.21.11
    extends Avatar implements ContainerUser, IdentityCarrier {
    //? if < 1.21.11
    //extends LivingEntity implements IdentityCarrier {

    @Unique
    private boolean armorHider$needsArmRerender;

    @Unique
    private boolean armorHider$modsDirty = true;
    @Unique
    private PlayerModificationInfo armorHider$playerModInfo;

    public PlayerModificationInfo armorHider$getPlayerModifications(){
        return armorHider$playerModInfo;
    }

    @Unique
    private Consumer<@Nullable String> armorHider$configListener = (changedPlayerName) -> {
        if (changedPlayerName == null || changedPlayerName.equals(armorHider$playerName())) {
            armorHider$modsDirty = true;
        }
    };

    protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void registerConfigListener(CallbackInfo ci) {
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER != null) {
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.addConfigChangeListener(armorHider$configListener);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void unregisterConfigListener(Entity.RemovalReason reason, CallbackInfo ci) {
        if (armorHider$configListener != null) {
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.removeConfigChangeListener(armorHider$configListener);
            armorHider$configListener = null;
        }
    }

    @Inject(method = "onEquipItem", at = @At("HEAD"))
    private void markModsDirtyOnEquipChange(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, CallbackInfo ci) {
        if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
            armorHider$modsDirty = true;
        }
    }

    @Unique
    private void armorHider$rebuildModsIfDirty() {
        if (!armorHider$modsDirty) {
            return;
        }
        DebugLogger.log("Rebuilding armor mods for " + armorHider$playerName());
        armorHider$modsDirty = false;
        armorHider$playerModInfo = new PlayerModificationInfo(
                getModification(EquipmentSlot.HEAD, getItemBySlot(EquipmentSlot.HEAD)),
                getModification(EquipmentSlot.CHEST, getItemBySlot(EquipmentSlot.CHEST)),
                getModification(EquipmentSlot.LEGS, getItemBySlot(EquipmentSlot.LEGS)),
                getModification(EquipmentSlot.FEET, getItemBySlot(EquipmentSlot.FEET)),
                customHeadItem()
        );
    }

    @Unique
    private boolean armorHider$isCombatActive() {
        String name = armorHider$playerName();
        return name != null && ArmorHiderApi.getInstance().getCombatManagement().isInCombat(name);
    }

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

        var ctx = ArmorHiderClientApi.getInstance().getRenderingScopeApi();

        if (ctx.hasActiveModification()) {
            return original;
        }

        // Only fake empty slots during level rendering (3D world) — never during
        // game logic (tick processing, inventory interactions) or HUD/GUI rendering.
        if (!ctx.isInLevelRender()) {
            return original;
        }

        var playerName = armorHider$playerName();
        // During entity rendering (extractRenderState + layer rendering), return the
        // real item so that renderArmorPiece is called (for downstream render processing).
        if (ctx.isInEntityRender() || playerName == null) {
            return original;
        }

        if (ActiveModification.isSlotFullyHidden(playerName, slot, original)) {
            DebugTracer.equipmentSlotHidingFired(playerName, slot, true, "isSlotFullyHidden");
            return ItemStack.EMPTY;
        }
        return original;
    }
}
