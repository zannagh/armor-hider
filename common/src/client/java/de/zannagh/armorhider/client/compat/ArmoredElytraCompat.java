package de.zannagh.armorhider.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

//? if >= 1.21.2 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
//?}

/**
 * Compatibility for the "Armored Elytra" mod (dorkix, Modrinth {@code elytra-armor}).
 * <p>
 * That mod forges a chestplate + elytra into a single worn item that is a plain {@link Items#ELYTRA}
 * carrying the original chestplate (and elytra) as NBT inside its {@code CUSTOM_DATA}. On the client it
 * swaps the stored chestplate back in at {@code HumanoidArmorLayer.renderArmorPiece} so the body plate
 * draws - but Female Gender Mod's {@code GenderArmorLayer} is a separate layer that reads the raw worn
 * item, sees an elytra (whose equipment asset has no humanoid layer), and therefore skips the breast
 * armor entirely. The result: with an armored elytra worn, FGM breast armor vanishes.
 * <p>
 * This helper resolves the stored chestplate so the FGM compat mixin can substitute it for the asset
 * lookup, letting FGM draw the breast armor again (which Armor Hider's own breast hooks then fade).
 * The CUSTOM_DATA key is the mod's public forge format; if the item is not such an armored elytra the
 * original stack is returned unchanged.
 */
public final class ArmoredElytraCompat {

    // The mod stores the chestplate under Identifier("armored_elytra", "chestplate").toString().
    private static final String CHESTPLATE_DATA_KEY = "armored_elytra:chestplate";

    private ArmoredElytraCompat() {}

    /**
     * @param stack the worn chest item; may be {@code null}.
     * @return the chestplate stored inside a dorkix armored elytra; {@code stack} unchanged when it is
     *         not one (mod absent, not an elytra, no stored chestplate, or the NBT can't be decoded);
     *         or {@link ItemStack#EMPTY} when {@code stack} is {@code null}.
     */
    public static ItemStack underlyingChestplateOrSelf(@Nullable ItemStack stack) {
        //? if >= 1.21.2 {
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        // Only act when the Armored Elytra mod is actually loaded. Without it, an item carrying this
        // CUSTOM_DATA is just a plain elytra (nothing draws its body chestplate), so forcing FGM to draw
        // chest/breast armor off the leftover data would be wrong - leave such items untouched.
        if (!de.zannagh.armorhider.api.compat.CompatManager.requiresCompatTo(
                de.zannagh.armorhider.api.compat.CompatFlags.ARMORED_ELYTRA)) {
            return stack;
        }
        if (!stack.is(Items.ELYTRA)) {
            return stack;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return stack;
        }
        // Read the component directly and bail before copying its tag when the stack has no CUSTOM_DATA
        // (the common case) - copyTag() allocates, and this runs on the render path via the FGM mixins.
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return stack;
        }
        // CompoundTag#get(String) returns a nullable Tag on every supported version, avoiding the
        // getCompound() return-type split (raw CompoundTag on older versions, Optional on newer ones).
        Tag chestTag = customData.copyTag().get(CHESTPLATE_DATA_KEY);
        if (!(chestTag instanceof CompoundTag chestCompound) || chestCompound.isEmpty()) {
            return stack;
        }
        var ops = RegistryOps.create(NbtOps.INSTANCE, client.player.registryAccess());
        ItemStack chestplate = ItemStack.CODEC.parse(ops, chestCompound).resultOrPartial().orElse(ItemStack.EMPTY);
        if (chestplate.isEmpty() || chestplate.is(Items.ELYTRA)) {
            return stack;
        }
        return chestplate;
        //?} else
        /*return stack == null ? ItemStack.EMPTY : stack;*/
    }
}
