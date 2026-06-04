package de.zannagh.armorhider.client.api.implementations;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.configuration.SlotModification;
import de.zannagh.armorhider.client.api.render.AhRenderInterceptionApi;
import de.zannagh.armorhider.client.api.render.RenderInterceptionResult;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.common.ItemInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

public class AhRenderInterceptionApiImpl implements AhRenderInterceptionApi {
    @Override
    public RenderInterceptionResult interceptRenderCall(InterceptionContext context, Object... additionalContext) {
        if (context == InterceptionContext.PER_PIECE_LAYER || context == InterceptionContext.PER_PIECE_LAYER_WITHOUT_CONTEXT_SET || context == InterceptionContext.PER_PLAYER_CAPTURE) {
            boolean setContext = context == InterceptionContext.PER_PIECE_LAYER;
            var objects = Arrays.asList(additionalContext);
            ItemInfo itemInfo = ItemInfo.empty();
            if (objects.isEmpty()) {
                return RenderInterceptionResult.shouldUseVanilla();
            }
            if (objects.get(0) instanceof IdentityCarrier carrier) {
                ArmorHiderClientApi.getInstance().getRenderingScopeApi().setCurrentPlayer(carrier.armorHider$playerName());
                if (objects.size() == 1 || context == InterceptionContext.PER_PLAYER_CAPTURE) {
                    if (carrier.armorHider$allSlotsFullyHidden()) {
                        return new RenderInterceptionResult(false, true, carrier, ItemStack.EMPTY, EquipmentSlot.MAINHAND);
                    }
                    return new RenderInterceptionResult(true, false, carrier, ItemStack.EMPTY, EquipmentSlot.MAINHAND);
                }
                if (objects.size() > 2 && objects.get(2) instanceof ItemStack itemStack) {
                    itemInfo = new ItemInfo(itemStack);
                }
                else if (objects.get(1) instanceof EquipmentSlot slot) {
                    itemInfo = new ItemInfo(carrier.getItemBySlot(slot));
                }
                if (itemInfo.isEmpty()) {
                    return RenderInterceptionResult.shouldUseVanilla();
                }
                var slot = objects.size() > 2 ? (EquipmentSlot) objects.get(2) : itemInfo.getEquippableSlot();
                if (itemInfo.isElytra() && carrier.isPlayerFlying()) {
                    return new RenderInterceptionResult(false, false, carrier, itemInfo.getStack(), slot);
                }
                if (slot == null) {
                    return new RenderInterceptionResult(false, false, carrier, itemInfo.getStack(), null);
                }
                var slotMod = setContext
                        ? carrier.createModificationAndSetContext(slot, itemInfo.getStack())
                        : SlotModification.of(carrier.armorHider$playerName(), slot, itemInfo.getStack());
                return new RenderInterceptionResult(true, slotMod.shouldHide(), carrier, itemInfo.getStack(), slot);
            }
            else {
                var currentScope = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
                if (currentScope.currentlyHandledPlayerName().isBlank()) {
                    return RenderInterceptionResult.shouldUseVanilla();
                }
                if (objects.get(2) instanceof ItemStack itemStack) {
                    itemInfo = new ItemInfo(itemStack);
                }
                if (objects.get(1) instanceof EquipmentSlot slot) {
                    var mod = SlotModification.of(currentScope.currentlyHandledPlayerName(), slot, itemInfo.getStack());
                    if (setContext) {
                        ArmorHiderClientApi.getInstance().getRenderingScopeApi().setActiveModification(mod);
                    }
                }
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
