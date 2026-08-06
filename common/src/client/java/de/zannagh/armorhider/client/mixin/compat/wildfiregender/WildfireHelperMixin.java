//? if gender {
package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import de.zannagh.armorhider.client.compat.ArmoredElytraCompat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

//? if >= 1.21 {
import com.wildfire.main.WildfireHelper;
//?}

/**
 * Armored Elytra (dorkix) compat for Female Gender Mod.
 * <p>
 * {@code WildfireHelper.getArmorConfig(ItemStack)} is FGM's single item to {@code IGenderArmor}
 * resolver: {@code GenderLayer} feeds the worn chest item through it and the resulting
 * {@code genderArmor.coversBreasts()} decides whether breast armor is drawn. A dorkix armored elytra
 * is a plain {@link net.minecraft.world.item.Items#ELYTRA} whose stored chestplate lives in
 * CUSTOM_DATA, so this resolves to a non-covering armor and the breast armor vanishes. Substituting
 * the stored chestplate here (and only here - non-armored-elytra stacks pass through unchanged) makes
 * FGM resolve the chestplate's armor config, so the breast armor is drawn again with its texture;
 * Armor Hider's own breast hooks then apply the configured chest opacity. Paired with the
 * {@code get(EQUIPPABLE)} wrap in {@code GenderArmorLayerMixin}, which clears FGM's separate
 * humanoid-layer check on the same render.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
//? if >= 1.21
@Mixin(value = WildfireHelper.class, remap = false)
//? if < 1.21
/*@Mixin(targets = "com.wildfire.main.WildfireHelper", remap = false)*/
public class WildfireHelperMixin {

    // require = 0 matches the rest of this @Pseudo FGM compat (silent no-op if FGM's helper drifts);
    // ArmoredElytraGenderSmokeTest pins the target.
    @ModifyVariable(method = "getArmorConfig", at = @At("HEAD"), argsOnly = true, require = 0)
    private static ItemStack armorHider$armoredElytraChest(ItemStack stack) {
        return ArmoredElytraCompat.underlyingChestplateOrSelf(stack);
    }
}
//?}
