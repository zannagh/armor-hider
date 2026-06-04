package de.zannagh.armorhider.client.api.implementations;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.configuration.ScopeHandover;
import de.zannagh.armorhider.client.api.configuration.SlotModification;
import de.zannagh.armorhider.client.api.render.AhRenderInterceptionApi;
import de.zannagh.armorhider.client.api.render.RenderInterceptionResult;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.common.ItemInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


public class AhRenderInterceptionApiImpl implements AhRenderInterceptionApi {

    private boolean allInterceptionArgumentsEmpty(@Nullable IdentityCarrier carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack) {
        return carrier == null && slot == null && stack == null;
    }

    @Override
    public RenderInterceptionResult interceptRenderCall(InterceptionContext context, @Nullable IdentityCarrier carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, LocalRef<ScopeHandover> carrierRef) {
        ItemInfo itemInfo = new ItemInfo(stack);
        if (!itemInfo.isEmpty() && slot == null) {
            slot = itemInfo.getEquippableSlot();
        }

        boolean setContext = context == InterceptionContext.PER_PIECE_LAYER;

        if (allInterceptionArgumentsEmpty(carrier, slot, stack)) {
            return RenderInterceptionResult.shouldUseVanilla();
        }
        if (carrier != null) {
            if (itemInfo.isEmpty() && slot != null) {
                itemInfo = new ItemInfo(carrier.getItemBySlot(slot));
            }
            ArmorHiderClientApi.getInstance().getRenderingScopeApi().setCurrentPlayer(carrier.armorHider$playerName());
            if ((slot == null && itemInfo.isEmpty()) || context == InterceptionContext.PER_PLAYER_CAPTURE) {
                if (carrier.armorHider$allSlotsFullyHidden()) {
                    carrierRef.set(new ScopeHandover(carrier, null));
                    return new RenderInterceptionResult(false, true, carrier, ItemStack.EMPTY, EquipmentSlot.MAINHAND);
                }
                carrierRef.set(new ScopeHandover(carrier, null));
                return new RenderInterceptionResult(true, false, carrier, ItemStack.EMPTY, EquipmentSlot.MAINHAND);
            }

            if (itemInfo.isEmpty()) {
                carrierRef.set(new ScopeHandover(carrier, null));
                return RenderInterceptionResult.shouldUseVanilla();
            }
            var equipmentSlot = slot != null ? slot : itemInfo.getEquippableSlot();
            if (itemInfo.isElytra() && carrier.isPlayerFlying()) {
                carrierRef.set(new ScopeHandover(carrier, null));
                return new RenderInterceptionResult(false, false, carrier, itemInfo.getStack(), equipmentSlot);
            }
            if (slot == null) {
                carrierRef.set(new ScopeHandover(carrier, null));
                return new RenderInterceptionResult(false, false, carrier, itemInfo.getStack(), null);
            }
            var slotMod = setContext
                    ? carrier.createModificationAndSetContext(slot, itemInfo.getStack())
                    : SlotModification.of(carrier.armorHider$playerName(), slot, itemInfo.getStack());
            carrierRef.set(new ScopeHandover(carrier, slotMod));
            return new RenderInterceptionResult(true, slotMod.shouldHide(), carrier, itemInfo.getStack(), slot);
        }
        else {
            var currentScope = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
            if (currentScope.currentlyHandledPlayerName().isBlank()) {
                return RenderInterceptionResult.shouldUseVanilla();
            }
            if (slot != null) {
                var mod = SlotModification.of(currentScope.currentlyHandledPlayerName(), slot, itemInfo.getStack());
                if (setContext) {
                    ArmorHiderClientApi.getInstance().getRenderingScopeApi().setActiveModification(mod);
                }
                carrierRef.set(new ScopeHandover(null, mod));
            }
        }

        return RenderInterceptionResult.shouldUseVanilla();
    }

    @Override
    public void wrapAndCancelRenderCall(CallbackInfo ci) {
        ci.cancel();
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().clearActiveModification();
    }
}
