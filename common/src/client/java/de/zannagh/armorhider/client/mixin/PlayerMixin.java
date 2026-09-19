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

@Mixin(Player.class)
public abstract class PlayerMixin
    //? if >= 1.21.11
    extends Avatar implements ContainerUser, IdentityCarrier {
    //? if < 1.21.11
    //extends LivingEntity implements IdentityCarrier {

    @Unique
    private boolean armorHider$modsDirty = true;
    @Unique
    private PlayerModificationInfo armorHider$playerModInfo;

    @Unique
    private long armorHider$seenConfigGeneration = -1;

    public PlayerModificationInfo armorHider$getPlayerModifications() {
        armorHider$rebuildModsIfDirty();
        return armorHider$playerModInfo;
    }

    protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
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
        long gen = ArmorHiderClient.CLIENT_CONFIG_MANAGER == null
                ? 0
                : ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigGeneration();
        if (!armorHider$modsDirty && gen == armorHider$seenConfigGeneration) {
            return;
        }
        DebugLogger.log("Rebuilding armor mods for " + armorHider$playerName());
        armorHider$seenConfigGeneration = gen;
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
        // getItemBySlot is called constantly by game logic as well as by rendering, so every bail-out is
        // ordered cheapest-first. All of them return `original` unchanged, which is what makes the order
        // free to choose: the only path that returns something else is the final hidden-slot check, and it
        // is reached only when every guard below has passed. Resolving the scope (RenderScope.of -> new
        // ItemInfo -> isElytra) used to happen before the two pure flag reads; it now happens after them.
        if (original.isEmpty()) {
            return original;
        }

        // Only fake empty slots during level rendering (3D world) - never during
        // game logic (tick processing, inventory interactions) or HUD/GUI rendering.
        if (!AhRenderManagementApi.isInLevelRender()) {
            return original;
        }

        // During entity rendering (extractRenderState + layer rendering), return the
        // real item so that renderArmorPiece is called (for downstream render processing).
        if (AhRenderManagementApi.isInEntityRender()) {
            return original;
        }

        var scope = RenderScope.of(slot, new ItemInfo(original));
        if (AhRenderManagementApi.hasScopeModification(scope)) {
            return original;
        }

        var playerName = armorHider$playerName();
        if (playerName == null) {
            return original;
        }

        if (AhRenderManagementApi.getActiveScope(scope).renderModificationApi().isSlotFullyHiddenForPlayer(playerName, slot, original)) {
            DebugTracer.equipmentSlotHidingFired(playerName, slot, true, "isSlotFullyHidden");
            return ItemStack.EMPTY;
        }
        return original;
    }
}
