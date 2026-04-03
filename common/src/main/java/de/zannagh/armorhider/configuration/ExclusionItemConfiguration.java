package de.zannagh.armorhider.configuration;

import de.zannagh.armorhider.ArmorHider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player configuration of which items the mod should handle or ignore,
 * organized by equipment slot. Uses string-based item registry IDs (e.g.
 * "minecraft:diamond_helmet") as keys for reliable serialization.
 * <p>
 * Default behavior: items NOT in the list are intercepted (mod handles them).
 * Items in the list with {@code shouldIgnore = true} are skipped by the mod.
 */
public class ExclusionItemConfiguration {

    /**
     * Slot name → item registry ID → exclusion info.
     * Uses String keys for both slot and item to ensure clean GSON serialization.
     */
    Map<String, Map<String, ExclusionItemInfo>> items = new LinkedHashMap<>();

    /**
     * Caches Item → registry ID lookups so that non-registry or slow lookups
     * only happen once. Shared across all instances since the mapping is global.
     */
    private static final ConcurrentHashMap<Item, String> ITEM_ID_CACHE = new ConcurrentHashMap<>();

    public static ExclusionItemConfiguration deserialize(Reader reader) {
        return ArmorHider.GSON.fromJson(reader, ExclusionItemConfiguration.class);
    }

    public static ExclusionItemConfiguration deserialize(String content) {
        return ArmorHider.GSON.fromJson(content, ExclusionItemConfiguration.class);
    }

    /**
     * Returns a deep copy of this configuration.
     */
    public ExclusionItemConfiguration deepCopy() {
        var copy = new ExclusionItemConfiguration();
        for (Map.Entry<String, Map<String, ExclusionItemInfo>> slotEntry : items.entrySet()) {
            var slotCopy = new LinkedHashMap<String, ExclusionItemInfo>();
            for (Map.Entry<String, ExclusionItemInfo> itemEntry : slotEntry.getValue().entrySet()) {
                ExclusionItemInfo orig = itemEntry.getValue();
                slotCopy.put(itemEntry.getKey(), new ExclusionItemInfo(orig.displayName, orig.shouldIgnore));
            }
            copy.items.put(slotEntry.getKey(), slotCopy);
        }
        return copy;
    }

    /**
     * Returns true if the mod should intercept (handle) this item.
     * Items not in the list default to intercepted.
     */
    public boolean shouldIntercept(EquipmentSlot slot, Item item) {
        return !shouldArmorHiderIgnore(slot, item);
    }

    /**
     * Returns true if the mod should ignore (skip) this item.
     * Items not in the list default to NOT ignored (mod handles them).
     */
    public boolean shouldArmorHiderIgnore(EquipmentSlot slot, Item item) {
        String itemId = getItemId(item);
        Map<String, ExclusionItemInfo> slotItems = items.get(slot.name());
        if (slotItems == null) return false;
        ExclusionItemInfo info = slotItems.get(itemId);
        if (info == null) return false;
        return info.shouldIgnore;
    }

    /**
     * Returns all items configured for a given slot.
     */
    public Map<String, ExclusionItemInfo> getItemsForSlot(EquipmentSlot slot) {
        return items.getOrDefault(slot.name(), Map.of());
    }

    /**
     * Sets or updates an item's exclusion info for a slot.
     */
    public void setItem(EquipmentSlot slot, String itemId, ExclusionItemInfo info) {
        items.computeIfAbsent(slot.name(), s -> new LinkedHashMap<>()).put(itemId, info);
    }

    /**
     * Toggles the interception state for an item in a slot.
     * Returns the new shouldIgnore state, or null if the item wasn't found.
     */
    public Boolean toggleItem(EquipmentSlot slot, String itemId) {
        Map<String, ExclusionItemInfo> slotItems = items.get(slot.name());
        if (slotItems == null) return null;
        ExclusionItemInfo info = slotItems.get(itemId);
        if (info == null) return null;
        info.shouldIgnore = !info.shouldIgnore;
        return info.shouldIgnore;
    }

    /**
     * If an item is not yet tracked for its slot, adds it with interception enabled.
     * Used for auto-discovery of new equippable items during rendering.
     *
     * @return true if the item was newly added
     */
    public synchronized boolean discoverItem(EquipmentSlot slot, Item item) {
        String itemId = getItemId(item);
        Map<String, ExclusionItemInfo> slotItems = items.get(slot.name());
        if (slotItems != null && slotItems.containsKey(itemId)) {
            return false;
        }
        setItem(slot, itemId, ExclusionItemInfo.intercepted(itemId));
        return true;
    }

    /**
     * Same as {@link #discoverItem(EquipmentSlot, Item)} but accepts a
     * pre-resolved display name (e.g. from ItemStack.getHoverName on the client).
     */
    public synchronized boolean discoverItem(EquipmentSlot slot, Item item, String displayName) {
        String itemId = getItemId(item);
        Map<String, ExclusionItemInfo> slotItems = items.get(slot.name());
        if (slotItems != null && slotItems.containsKey(itemId)) {
            return false;
        }
        setItem(slot, itemId, ExclusionItemInfo.intercepted(displayName));
        return true;
    }

    /**
     * Converts an Item to its registry ID string (e.g. "minecraft:diamond_helmet").
     * Results are cached so that subsequent lookups for the same Item are instant.
     * If the registry lookup fails (e.g. for unregistered mod items), falls back
     * to a synthetic ID based on the item's class name and identity hash.
     */
    public static String getItemId(Item item) {
        return ITEM_ID_CACHE.computeIfAbsent(item, i -> {
            try {
                var key = BuiltInRegistries.ITEM.getKey(i);
                String id = key.toString();
                // BuiltInRegistries returns "minecraft:air" for unknown items
                if (!"minecraft:air".equals(id) || i == net.minecraft.world.item.Items.AIR) {
                    return id;
                }
            } catch (Exception e) {
                ArmorHider.LOGGER.warn("Failed to resolve registry ID for item {}: {}", i, e.getMessage());
            }
            // Fallback for items not in the registry
            return "unknown:" + i.getClass().getSimpleName().toLowerCase() + "_" + System.identityHashCode(i);
        });
    }

    /**
     * Looks up an Item by its registry ID string.
     * Returns the item, or Items.AIR if not found or if the lookup fails.
     */
    public static Item getItemFromId(String itemId) {
        try {
            //? if >= 1.21.4 {
            return BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(itemId));
            //?} else if >= 1.20.5 {
            /*return BuiltInRegistries.ITEM.get(net.minecraft.resources.Identifier.parse(itemId));
            *///?} else {
            /*return BuiltInRegistries.ITEM.get(new net.minecraft.resources.Identifier(itemId));
            *///?}
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to resolve item from ID '{}': {}", itemId, e.getMessage());
            return net.minecraft.world.item.Items.AIR;
        }
    }

    public static ExclusionItemConfiguration defaults() {
        var config = new ExclusionItemConfiguration();

        // HEAD slot
        addDefaults(config, EquipmentSlot.HEAD,
                "minecraft:leather_helmet", "Leather Helmet",
                "minecraft:chainmail_helmet", "Chainmail Helmet",
                "minecraft:iron_helmet", "Iron Helmet",
                "minecraft:golden_helmet", "Golden Helmet",
                "minecraft:diamond_helmet", "Diamond Helmet",
                "minecraft:netherite_helmet", "Netherite Helmet",
                "minecraft:turtle_helmet", "Turtle Helmet"
        );

        // CHEST slot
        addDefaults(config, EquipmentSlot.CHEST,
                "minecraft:leather_chestplate", "Leather Chestplate",
                "minecraft:chainmail_chestplate", "Chainmail Chestplate",
                "minecraft:iron_chestplate", "Iron Chestplate",
                "minecraft:golden_chestplate", "Golden Chestplate",
                "minecraft:diamond_chestplate", "Diamond Chestplate",
                "minecraft:netherite_chestplate", "Netherite Chestplate",
                "minecraft:elytra", "Elytra"
        );

        // LEGS slot
        addDefaults(config, EquipmentSlot.LEGS,
                "minecraft:leather_leggings", "Leather Leggings",
                "minecraft:chainmail_leggings", "Chainmail Leggings",
                "minecraft:iron_leggings", "Iron Leggings",
                "minecraft:golden_leggings", "Golden Leggings",
                "minecraft:diamond_leggings", "Diamond Leggings",
                "minecraft:netherite_leggings", "Netherite Leggings"
        );

        // FEET slot
        addDefaults(config, EquipmentSlot.FEET,
                "minecraft:leather_boots", "Leather Boots",
                "minecraft:chainmail_boots", "Chainmail Boots",
                "minecraft:iron_boots", "Iron Boots",
                "minecraft:golden_boots", "Golden Boots",
                "minecraft:diamond_boots", "Diamond Boots",
                "minecraft:netherite_boots", "Netherite Boots"
        );
        
        addDefaults(config, EquipmentSlot.OFFHAND,
                "minecraft:shield", "Shield");

        return config;
    }

    /**
     * Helper: adds pairs of (itemId, displayName) as intercepted items for a slot.
     */
    private static void addDefaults(ExclusionItemConfiguration config, EquipmentSlot slot, String... pairs) {
        for (int i = 0; i < pairs.length; i += 2) {
            config.setItem(slot, pairs[i], ExclusionItemInfo.intercepted(pairs[i + 1]));
        }
    }
}
