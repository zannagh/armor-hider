package de.zannagh.armorhider.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SkullBlock;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

//? if >= 1.21.4
import net.minecraft.core.component.DataComponents;
//? if < 1.21.4
//import net.minecraft.world.item.Equipable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ItemInfo {

    private static final Set<Item> SKULL_BLOCK_ITEMS = new HashSet<>(
            Arrays.asList(
                    Items.SKELETON_SKULL,
                    Items.DRAGON_HEAD,
                    Items.WITHER_SKELETON_SKULL,
                    Items.PLAYER_HEAD,
                    Items.ZOMBIE_HEAD,
                    Items.CREEPER_HEAD,
                    Items.PIGLIN_HEAD));

    private static final ItemStack ELYTRA_ITEM_STACK = new ItemStack(Items.ELYTRA);

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
            return null;
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
        //? if >= 1.21.4 {
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
        return itemStack.is(ELYTRA_ITEM_STACK.getItem()) || itemStack.getItem().toString().toLowerCase().contains("elytra");
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
}
