package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.EquippableInformation;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.common.ItemInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Default renderer for {@link RenderScope#ARMOR_PIECE}. Drives the {@code EquipmentLayerRenderer} /
 * {@code HumanoidArmorLayer} interception path: builds the modification from the slot+stack the caller
 * passes in, and carves out the elytra-while-flying case (cape/body handle that combo elsewhere).
 */
public class ArmorHiderItemRenderer extends AbstractArmorHiderRenderer {

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.ARMOR_PIECE;
    }

    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        IdentityCarrier carrier = identityCarrier instanceof IdentityCarrier ic ? ic : null;
        var equipmentInfo = new EquippableInformation(identityCarrier, slot, stack);
        if (!equipmentInfo.isValid()) {
            setEmptyModification();
            return RenderInterceptionResult.ignore();
        }
        EquipmentSlot resolvedSlot = equipmentInfo.getSlot();
        if (stack == null) {
            stack = equipmentInfo.getStack();
        }

        // A plain elytra worn in the chest is wings-only, so hand it to the ELYTRA renderer. An
        // "armored elytra" (glider + a real chest armor asset - e.g. the Elytra Armor datapack, or a
        // chestplate given a glider component) is NOT delegated: this renderer is only asked to drive
        // body-armor geometry (the vanilla plate and Female Gender Mod's breast armor), which belongs in
        // ARMOR_PIECE. Delegating it to ELYTRA scoped the breast wrong, so its render-type swap (keyed on
        // ARMOR_PIECE) never fired and the piece stayed invisible through combat while the scope leaked.
        // The wings of an armored elytra still reach the ELYTRA scope via their own WingsLayer render.
        if (equipmentInfo.getItemInfo().isElytra() && !equipmentInfo.getItemInfo().isArmoredElytra()) {
            var elytraRenderer = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.ELYTRA);
            return elytraRenderer.intercept(identityCarrier, resolvedSlot, stack, ci);
        }

        if (carrier != null) {
            var mod = resolveModification(carrier, resolvedSlot, stack);
            var itemInfo = mod.itemInfo() != null ? mod.itemInfo() : new ItemInfo(stack);
            if (itemInfo.isElytra() && carrier.isPlayerFlying()) {
                return RenderInterceptionResult.ignore();
            }
            return super.standardIntercept(carrier, resolvedSlot, stack, ci);
        }

        setEmptyModification();
        return RenderInterceptionResult.ignore();
    }
}
