package de.zannagh.armorhider.configuration;

/**
 * Stores per-item exclusion state: whether the mod should intercept (handle)
 * or ignore (skip) a specific item in a given equipment slot.
 */
public class ExclusionItemInfo {
    /**
     * Human-readable display name for the item (used in config file readability).
     * The UI uses the actual localized item name instead.
     */
    public String displayName;

    /**
     * If true, the mod will NOT modify this item's rendering.
     * If false (default), the mod handles the item normally.
     */
    public boolean shouldIgnore;

    public ExclusionItemInfo() {
        this.displayName = "";
        this.shouldIgnore = false;
    }

    public ExclusionItemInfo(String displayName, boolean shouldIgnore) {
        this.displayName = displayName;
        this.shouldIgnore = shouldIgnore;
    }

    public boolean shouldIntercept() {
        return !shouldIgnore;
    }

    public static ExclusionItemInfo ignored(String displayName) {
        return new ExclusionItemInfo(displayName, true);
    }

    public static ExclusionItemInfo intercepted(String displayName) {
        return new ExclusionItemInfo(displayName, false);
    }
}
