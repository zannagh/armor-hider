package de.zannagh.armorhider.common;


import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SkullBlock;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

//? if >= 1.21.2 {
import net.minecraft.core.component.DataComponents;
//?}
//? if >= 1.21.4 {
import net.minecraft.world.item.equipment.EquipmentAssets;
//?}
//? if >= 1.21.2 && < 1.21.4
//import net.minecraft.resources.ResourceLocation;
//? if < 1.21.2
//import net.minecraft.world.item.Equipable;

import java.util.Locale;
import java.util.Set;

public class ItemInfo {

    private static final Set<Item> SKULL_BLOCK_ITEMS = Set.of(
            Items.SKELETON_SKULL,
            Items.DRAGON_HEAD,
            Items.WITHER_SKELETON_SKULL,
            Items.PLAYER_HEAD,
            Items.ZOMBIE_HEAD,
            Items.CREEPER_HEAD,
            Items.PIGLIN_HEAD);

    // Placeholder elytra stack, built lazily and cached only on first *successful* construction.
    // This class can be loaded - and elytraItemStack() called - before item registries / data
    // components are bound: some UI / picture-in-picture mods drive an early render on the render
    // thread while ELYTRA's Holder is still unbound, and building the stack then throws
    // "Components not bound yet" (issue #260). A static-holder <clinit> would cache that failure
    // permanently (ExceptionInInitializerError, then NoClassDefFoundError for the rest of the
    // session - breaking all elytra handling and crashing the render), so we build on demand,
    // cache the result once it succeeds, and fall back to an empty stack until the registry is
    // ready, retrying on the next call. Benign double-build race only; ItemStack was already shared.
    private static volatile ItemStack elytraStack;

    @NonNull private final ItemStack itemStack;

    public ItemInfo(@Nullable ItemStack itemStack){
        if (itemStack == null) {
            this.itemStack = ItemStack.EMPTY;
            return;
        }
        this.itemStack = itemStack;
    }

    public static ItemInfo empty() { return new ItemInfo(ItemStack.EMPTY); }

    public static ItemInfo of(@Nullable SkullBlock.Type skullBlockType){
        if (skullBlockType == null) {
            return ItemInfo.empty();
        }
        //noinspection IfCanBeSwitch - does not work on Java 17.
        if (skullBlockType == SkullBlock.Types.SKELETON) {
            return new ItemInfo(new ItemStack(Items.SKELETON_SKULL));
        } else if (skullBlockType == SkullBlock.Types.DRAGON) {
            return new ItemInfo(new ItemStack(Items.DRAGON_HEAD));
        } else if (skullBlockType == SkullBlock.Types.WITHER_SKELETON) {
            return new ItemInfo(new ItemStack(Items.WITHER_SKELETON_SKULL));
        } else if (skullBlockType == SkullBlock.Types.PLAYER) {
            return new ItemInfo(new ItemStack(Items.PLAYER_HEAD));
        } else if (skullBlockType == SkullBlock.Types.ZOMBIE) {
            return new ItemInfo(new ItemStack(Items.ZOMBIE_HEAD));
        } else if (skullBlockType == SkullBlock.Types.CREEPER) {
            return new ItemInfo(new ItemStack(Items.CREEPER_HEAD));
        } else if (skullBlockType == SkullBlock.Types.PIGLIN) {
            return new ItemInfo(new ItemStack(Items.PIGLIN_HEAD));
        } else {
            return new ItemInfo(null);
        }
    }

    @Nullable
    public EquipmentSlot getEquippableSlot(){
        //? if >= 1.21.2 {
        var equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return null;
        }
        return equippable.slot();
        //?} else {
        /*var equippable = Equipable.get(itemStack);
        if (equippable == null) {
            return null;
        }
        return equippable.getEquipmentSlot();
        *///?}
    }

    public boolean isElytra() {
        if (itemStack.isEmpty()) {
            return false;
        }
        return itemStack.is(Items.ELYTRA)
                //? if >= 1.21.2 {
                || itemStack.getComponents().has(DataComponents.GLIDER)
                //? }
                || itemStack.getItem().toString().toLowerCase(Locale.ROOT).contains("elytra");
    }

    /**
     * Whether this item both glides ({@link #isElytra()}) and renders as chest body armor - a
     * "combined" item like the Elytra Armor datapack's forged elytra (an elytra worn in the chest that
     * also carries a humanoid armor asset), or any chestplate given a glider component. Distinguished
     * from a plain elytra by its equippable asset being a real armor asset rather than the elytra one.
     * <p>
     * Used to route rendering: a plain elytra is wings-only and belongs in the {@code ELYTRA} scope, but
     * an armored elytra also draws body armor (vanilla chestplate model + Female Gender Mod breast armor)
     * that must be handled in the {@code ARMOR_PIECE} scope. The concept does not exist before 1.21.2
     * (no glider/equippable-asset data components), so this is always {@code false} there.
     */
    public boolean isArmoredElytra() {
        //? if >= 1.21.4 {
        if (!isElytra()) {
            return false;
        }
        var equippable = itemStack.get(DataComponents.EQUIPPABLE);
        return equippable != null
                && equippable.slot() == EquipmentSlot.CHEST
                && equippable.assetId().isPresent()
                && !equippable.assetId().get().equals(EquipmentAssets.ELYTRA);
        //?} elif >= 1.21.2 {
        /*if (!isElytra()) {
            return false;
        }
        // 1.21.2/1.21.3 predate EquipmentAssets: the equippable exposes its model as a raw
        // ResourceLocation (later renamed assetId), and the plain elytra's model is minecraft:elytra.
        var equippable = itemStack.get(DataComponents.EQUIPPABLE);
        return equippable != null
                && equippable.slot() == EquipmentSlot.CHEST
                && equippable.model().isPresent()
                && !equippable.model().get().equals(ResourceLocation.withDefaultNamespace("elytra"));
        *///?} else {
        /*return false;
        *///?}
    }

    public boolean isVanillaSkullItem(){
        if (itemStack.isEmpty()) {
            return false;
        }
        return SKULL_BLOCK_ITEMS.contains(itemStack.getItem());
    }

    public boolean isEmpty() { return itemStack.isEmpty(); }

    public Item getItem() {
        return itemStack.getItem();
    }

    public ItemStack getStack() {
        return itemStack;
    }

    public static ItemInfo elytraItemInfo() {
        ItemStack cached = elytraStack;
        if (cached != null) {
            return new ItemInfo(cached);
        }
        try {
            ItemStack built = new ItemStack(Items.ELYTRA);
            elytraStack = built;
            return new ItemInfo(built);
        } catch (NullPointerException registryNotBoundYet) {
            // The known early-render failure: ItemStack's constructor hits Holder.Reference.components() ->
            // Objects.requireNonNull("Components not bound yet") while the registry is still binding (very early
            // PiP/GUI render). Suppress only this narrow NPE - don't cache the failure, return a harmless empty
            // stack and retry next call. Any other exception is a real bug and is allowed to propagate.
            return ItemInfo.empty();
        }
    }
}
