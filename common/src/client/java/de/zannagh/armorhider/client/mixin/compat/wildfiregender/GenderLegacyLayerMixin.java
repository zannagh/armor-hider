//? if gender_legacy {
/*package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.wildfire.render.GenderLayer;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.SlotModification;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/^*
 * Compatibility mixin for the legacy female-gender API (female-gender NeoForge 1.21/1.21.1/1.21.2/1.21.3,
 * hash {@code kKffHCGl} / 3.2.2), whose {@link GenderLayer} has a single monolithic {@code render} that
 * draws the breast BODY (skin-textured) and the breast ARMOR inline, with no separate
 * {@code renderBreastArmor} hook to intercept.
 * <p>
 * Inside {@code renderBreast}, the breast body boxes are drawn first, then the breast armor is drawn only
 * when the worn chest {@code ItemStack} is a non-empty {@code ArmorItem}. So when Armor Hider fully hides
 * the chest, we make {@code render}'s {@code getItemBySlot(CHEST)} lookup return an empty stack: FGM then
 * still draws the breast body (its {@code coversBreasts} visibility gate resolves the empty stack to a
 * non-covering config) but skips the breast armor - matching the hidden vanilla chestplate.
 * <p>
 * This deliberately does NOT cancel the whole layer (that hid the body too - the reported bug). Partial-
 * opacity <em>fade</em> of the breast armor is not supported on this build: the armor is drawn inline with
 * no clean per-draw alpha seam, so it can only be shown or fully hidden here (at 0% chest opacity).
 ^/
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(value = GenderLayer.class, remap = false)
public class GenderLegacyLayerMixin {

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;",
                    remap = true),
            require = 0
    )
    private ItemStack armorHider$hideBreastArmorWhenChestHidden(LivingEntity self, EquipmentSlot slot, Operation<ItemStack> original) {
        ItemStack stack = original.call(self, slot);
        if (slot == EquipmentSlot.CHEST && self instanceof IdentityCarrier carrier) {
            String playerName = carrier.armorHider$playerName();
            // Resolve the hide state LIVE (SlotModification.of) rather than the cached getModification -
            // an opacity-slider change does not mark that cache dirty, so it can read stale not-hidden.
            if (playerName != null && !playerName.isBlank()
                    && SlotModification.of(playerName, EquipmentSlot.CHEST, stack).shouldHide()) {
                // Empty chest -> FGM draws the breast body but skips the inline breast armor (its armor
                // draw is guarded by stack.isEmpty()/isArmorItem). The vanilla chestplate is hidden by
                // Armor Hider's core path, so the whole chest is consistently bare.
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }
}
*///?}
