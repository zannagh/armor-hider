package de.zannagh.armorhider.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SkullBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

//region ConditionalImports
//? if >= 1.21.9
//import net.minecraft.core.component.DataComponents;
//endregion

public final class ItemsUtil {
    public static final Set<Item> SKULL_BLOCK_ITEMS = new HashSet<>(
            Arrays.asList(
                    Items.SKELETON_SKULL,
                    Items.DRAGON_HEAD,
                    Items.WITHER_SKELETON_SKULL,
                    Items.PLAYER_HEAD,
                    Items.ZOMBIE_HEAD,
                    Items.CREEPER_HEAD,
                    Items.PIGLIN_HEAD));

    // Lazily created so this class can be loaded before item registries / data components are
    // bound. Some UI / picture-in-picture mods force early class loading, and eagerly building an
    // ItemStack at <clinit> then crashes with "Components not bound yet" (issue #260). The holder
    // defers construction to first use (render time, registries ready) and is thread-safe via the
    // JVM class-init lock.
    private static final class ElytraStackHolder {
        private static final ItemStack STACK = new ItemStack(Items.ELYTRA);
    }

    public static ItemStack elytraItemStack() {
        return ElytraStackHolder.STACK;
    }

    public static ItemStack getItemStackFromSkullBlockType(@Nullable SkullBlock.Type type) {
        if (type == null) {
            return ItemStack.EMPTY;
        }
        // Keeping this as a if/else chain, since <= 1.21 we can't use switch statements.
        if (type == SkullBlock.Types.SKELETON) {
            return new ItemStack(Items.SKELETON_SKULL);
        } else if (type == SkullBlock.Types.DRAGON) {
            return new ItemStack(Items.DRAGON_HEAD);
        } else if (type == SkullBlock.Types.WITHER_SKELETON) {
            return new ItemStack(Items.WITHER_SKELETON_SKULL);
        } else if (type == SkullBlock.Types.PLAYER) {
            return new ItemStack(Items.PLAYER_HEAD);
        } else if (type == SkullBlock.Types.ZOMBIE) {
            return new ItemStack(Items.ZOMBIE_HEAD);
        } else if (type == SkullBlock.Types.CREEPER) {
            return new ItemStack(Items.CREEPER_HEAD);
        } else if (type == SkullBlock.Types.PIGLIN) {
            return new ItemStack(Items.PIGLIN_HEAD);
        } else {
            return ItemStack.EMPTY;
        }
    }

    public static boolean isSkullBlockItem(Item item) {
        return SKULL_BLOCK_ITEMS.contains(item);
    }

    public static boolean itemStackContainsElytra(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        //? if < 1.21.9 {
        return itemStack.is(Items.ELYTRA) || itemStack.getItem().toString().toLowerCase().contains("elytra");
        //?} else
        //return itemStack.getComponents().has(DataComponents.GLIDER) || itemStack.is(Items.ELYTRA) || itemStack.getItem().toString().toLowerCase().contains("elytra");
    }
}
