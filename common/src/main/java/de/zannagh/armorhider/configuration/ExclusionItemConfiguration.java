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
 *
 * @since 0.10.0-pre.5
 */
public class ExclusionItemConfiguration {

    /**
     * Slot name → item registry ID → exclusion info.
     * Uses String keys for both slot and item to ensure clean GSON serialization.
     * <p>
     * Declared as concrete {@link LinkedHashMap} at both levels on purpose: Gson honours a concrete field
     * type and constructs {@code LinkedHashMap}s on deserialize, so iteration follows insertion (discovery)
     * order that {@link #prune()} relies on. Left as the {@code Map} interface, Gson would instead build its
     * own {@code LinkedTreeMap}; that happens to iterate in insertion order today, but pinning the type keeps
     * the guarantee from silently depending on a Gson internal across the wide MC/Gson version range we build.
     */
    LinkedHashMap<String, LinkedHashMap<String, ExclusionItemInfo>> items = new LinkedHashMap<>();

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
        if (items == null) {
            return copy;
        }
        // Null-tolerant: this is reached from ConfigPreset#applyTo/#fromPlayerConfig, whose exclusion map is
        // read out of armor-hider-presets.json by plain Gson and never passes through PlayerConfig.heal().
        for (var slotEntry : items.entrySet()) {
            if (slotEntry.getKey() == null || slotEntry.getValue() == null) {
                continue;
            }
            var slotCopy = new LinkedHashMap<String, ExclusionItemInfo>();
            for (Map.Entry<String, ExclusionItemInfo> itemEntry : slotEntry.getValue().entrySet()) {
                ExclusionItemInfo orig = itemEntry.getValue();
                if (itemEntry.getKey() == null || orig == null) {
                    continue;
                }
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
        // Routed through getItemsForSlot so it inherits the same null-safety: a corrupt "items": null (in a
        // config or preset that has not yet been through prune()/heal()) must not NPE a render-path lookup.
        ExclusionItemInfo info = getItemsForSlot(slot).get(getItemId(item));
        return info != null && info.shouldIgnore;
    }

    /**
     * Returns all items configured for a given slot.
     */
    public Map<String, ExclusionItemInfo> getItemsForSlot(EquipmentSlot slot) {
        if (items == null) {
            return Map.of();
        }
        // getOrDefault returns a *stored* null rather than the fallback, so an explicit "HEAD": null in the
        // JSON would otherwise hand callers a null map.
        Map<String, ExclusionItemInfo> slotItems = items.get(slot.name());
        return slotItems != null ? slotItems : Map.of();
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

    /** Prefix of the synthetic IDs minted by {@link #getItemId} when a registry lookup fails. */
    public static final String SYNTHETIC_ID_PREFIX = "unknown:";

    /**
     * Maximum number of discovered (non user-configured) entries retained per slot. Entries the user has
     * explicitly excluded are always kept and do not count against this budget.
     */
    public static final int MAX_DISCOVERED_ITEMS_PER_SLOT = 512;

    /**
     * Drops entries that can never match again and bounds the rest, returning the number of repairs made
     * (entries removed, plus one if a null backing map had to be materialised) — a non-zero result signals
     * the caller to persist the cleaned-up form.
     * <p>
     * Two problems are repaired here:
     * <ul>
     *   <li><b>Synthetic IDs.</b> {@link #getItemId} falls back to {@code "unknown:<class>_<identityHashCode>"}
     *       for items missing from the registry. Identity hash codes are not stable across JVM runs, so every
     *       launch mints fresh keys for the same items and the map grows without bound. None of these keys can
     *       ever be matched again, so they are pure garbage.</li>
     *   <li><b>Unbounded discovery.</b> {@link #discoverItem} appends every equipped item ever rendered, for
     *       every player seen. On a busy modded server that is effectively unbounded.</li>
     * </ul>
     * The backing map is a {@link LinkedHashMap} at both levels (see the {@link #items} field note), so
     * iteration follows insertion (discovery) order and trimming drops the oldest discovered entries first —
     * and that holds across a save/reload, not just for a freshly-built instance.
     */
    public synchronized int prune() {
        // This map is deserialized reflectively by Gson — unlike ConfigurationItem fields it is NOT covered
        // by ConfigurationSourceSerializer#initializeNullConfigFields — so explicit JSON nulls survive into
        // it verbatim. Since prune() is the repair pass called from PlayerConfig.heal(), it must treat those
        // nulls as more corruption to clean up rather than tripping over them: an NPE here propagates out of
        // deserialize() into PlayerConfigFileProvider's catch-all, which discards the whole config and writes
        // defaults — turning "heal the config" into "silently wipe the config".
        int removed = 0;
        if (items == null) {
            items = new LinkedHashMap<>();
            // Count the materialisation as a repair so the caller persists the fixed form. Returning 0 here
            // would let an "items": null corruption be re-read and re-repaired on every launch, never written
            // back, since PlayerConfig.heal() only flags the config dirty when prune() reports > 0.
            return 1;
        }

        var slotIterator = items.entrySet().iterator();
        while (slotIterator.hasNext()) {
            var slotEntry = slotIterator.next();
            Map<String, ExclusionItemInfo> slotItems = slotEntry.getValue();
            if (slotEntry.getKey() == null || slotItems == null) {
                slotIterator.remove();
                removed++;
                continue;
            }

            // Drop corrupt entries and synthetic identity-hash IDs in one pass.
            var itemIterator = slotItems.entrySet().iterator();
            while (itemIterator.hasNext()) {
                Map.Entry<String, ExclusionItemInfo> itemEntry = itemIterator.next();
                String itemId = itemEntry.getKey();
                if (itemId == null || itemEntry.getValue() == null || itemId.startsWith(SYNTHETIC_ID_PREFIX)) {
                    itemIterator.remove();
                    removed++;
                }
            }

            // Anything the user deliberately excluded is intent worth preserving, so only the passively
            // discovered remainder is subject to the cap.
            var discoveredKeys = new java.util.ArrayList<String>();
            for (Map.Entry<String, ExclusionItemInfo> itemEntry : slotItems.entrySet()) {
                if (!itemEntry.getValue().shouldIgnore) {
                    discoveredKeys.add(itemEntry.getKey());
                }
            }
            int excess = discoveredKeys.size() - MAX_DISCOVERED_ITEMS_PER_SLOT;
            for (int i = 0; i < excess; i++) {
                slotItems.remove(discoveredKeys.get(i));
                removed++;
            }
        }
        return removed;
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
            return SYNTHETIC_ID_PREFIX + i.getClass().getSimpleName().toLowerCase() + "_" + System.identityHashCode(i);
        });
    }

    /**
     * Looks up an Item by its registry ID string.
     * Returns the item, or Items.AIR if not found or if the lookup fails.
     */
    public static Item getItemFromId(String itemId) {
        try {
            //? if >= 1.21.2 {
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
