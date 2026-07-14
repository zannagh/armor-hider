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
import net.minecraft.core.component.DataComponents;
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

    // Placeholder elytra stack, built lazily and cached only on first *successful* construction.
    // This class can be loaded — and elytraItemStack() called — before item registries / data
    // components are bound: some UI / picture-in-picture mods drive an early render on the render
    // thread while ELYTRA's Holder is still unbound, and building the stack then throws
    // "Components not bound yet" (issue #260). A static-holder <clinit> would cache that failure
    // permanently (ExceptionInInitializerError, then NoClassDefFoundError for the rest of the
    // session — breaking all elytra handling and crashing the render), so we build on demand,
    // cache the result once it succeeds, and fall back to an empty stack until the registry is
    // ready, retrying on the next call. Benign double-build race only; ItemStack was already shared.
    private static volatile ItemStack elytraStack;

    public static ItemStack elytraItemStack() {
        ItemStack cached = elytraStack;
        if (cached != null) {
            return cached;
        }
        try {
            ItemStack built = new ItemStack(Items.ELYTRA);
            elytraStack = built;
            return built;
        } catch (NullPointerException registryNotBoundYet) {
            // The known early-render failure: ItemStack's constructor hits Holder.Reference.components() ->
            // Objects.requireNonNull("Components not bound yet") while the registry is still binding (very early
            // PiP/GUI render). Suppress only this narrow NPE — don't cache the failure, return a harmless empty
            // stack and retry next call. Any other exception is a real bug and is allowed to propagate.
            return ItemStack.EMPTY;
        }
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
        /*return itemStack.is(Items.ELYTRA) || itemStack.getItem().toString().toLowerCase().contains("elytra");
        *///?} else
        return itemStack.getComponents().has(DataComponents.GLIDER) || itemStack.is(Items.ELYTRA) || itemStack.getItem().toString().toLowerCase().contains("elytra");
    }
}
