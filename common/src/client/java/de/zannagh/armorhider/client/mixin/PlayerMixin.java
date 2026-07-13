// | --------------------------------------------------- |
// | This mechanic is inspired by Show Me Your Skin!     |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.common.PlayerModificationInfo;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.log.DebugTracer;
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Consumer;

@Mixin(Player.class)
public abstract class PlayerMixin
    //? if >= 1.21.11
    //extends Avatar implements ContainerUser, IdentityCarrier {
    //? if < 1.21.11
    extends LivingEntity implements IdentityCarrier {

    @Unique
    private boolean armorHider$modsDirty = true;
    @Unique
    private PlayerModificationInfo armorHider$playerModInfo;

    @Unique
    private UUID armorHider$configChangeListenerGuid = UUID.randomUUID();

    public PlayerModificationInfo armorHider$getPlayerModifications() {
        armorHider$rebuildModsIfDirty();
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
            armorHider$configChangeListenerGuid = ArmorHiderClient.CLIENT_CONFIG_MANAGER.addConfigChangeListener(armorHider$configListener);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void unregisterConfigListener(Entity.RemovalReason reason, CallbackInfo ci) {
        if (armorHider$configListener != null && ArmorHiderClient.CLIENT_CONFIG_MANAGER != null) {
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.removeConfigChangeListener(armorHider$configChangeListenerGuid);
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
        var name = armorHider$playerName();
        armorHider$playerModInfo = new PlayerModificationInfo(
                SlotModification.of(name, EquipmentSlot.HEAD, getItemBySlot(EquipmentSlot.HEAD)),
                SlotModification.of(name, EquipmentSlot.CHEST, getItemBySlot(EquipmentSlot.CHEST)),
                SlotModification.of(name, EquipmentSlot.LEGS, getItemBySlot(EquipmentSlot.LEGS)),
                SlotModification.of(name, EquipmentSlot.FEET, getItemBySlot(EquipmentSlot.FEET))
        );
    }

    @Unique
    private boolean armorHider$isCombatActive() {
        String name = armorHider$playerName();
        return name != null && ArmorHiderApi.getInstance().getCombatManagement().isInCombat(name);
    }

    @Override
    public @Nullable String armorHider$playerName() {
        return PlayerNameUtil.getPlayerName(this);
    }

    @Override
    @NonNull
    public ItemStack armorHider$getItemBySlot(EquipmentSlot slot) {
        return ((Player) (Object) this).getItemBySlot(slot);
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

    @Override
    public boolean armorHider$isPlayerInvisible() {
        Player player = (Player) (Object) this;
        return player.isInvisible() || player.hasEffect(MobEffects.INVISIBILITY);
    }

    @Override
    public boolean isPlayerBlocking() {
        Player player = (Player) (Object) this;
        return player.isBlocking();
    }

    @ModifyReturnValue(method = "getItemBySlot", at = @At("RETURN"))
    private ItemStack hideFullyHiddenSlot(ItemStack original, EquipmentSlot slot) {
        if (original.isEmpty()) {
            return original;
        }
        if (AhRenderManagementApi.hasScopeModification(RenderScope.of(slot, new ItemInfo(original)))) {
            return original;
        }

        // Only fake empty slots during level rendering (3D world) — never during
        // game logic (tick processing, inventory interactions) or HUD/GUI rendering.
        if (!AhRenderManagementApi.isInLevelRender()) {
            return original;
        }

        var playerName = armorHider$playerName();
        // During entity rendering (extractRenderState + layer rendering), return the
        // real item so that renderArmorPiece is called (for downstream render processing).
        if (AhRenderManagementApi.isInEntityRender() || playerName == null) {
            return original;
        }

        if (AhRenderManagementApi.getActiveScope(RenderScope.of(slot, new ItemInfo(original))).renderModificationApi().isSlotFullyHiddenForPlayer(playerName, slot, original)) {
            DebugTracer.equipmentSlotHidingFired(playerName, slot, true, "isSlotFullyHidden");
            return ItemStack.EMPTY;
        }
        return original;
    }
}
