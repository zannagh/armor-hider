package de.zannagh.armorhider.client.common;

/**
 * Duck interface implemented by {@code OptionsMixin}, so non-mixin code can drop Armor Hider's key
 * mappings from {@code Options.keyMappings} again.
 * <p>
 * The array is {@code final} in vanilla and only the mixin holds the {@code @Mutable @Shadow} handle
 * that can reassign it, so this is the only way to reach it from ordinary client code. It exists for
 * one case: {@link de.zannagh.armorhider.ArmorHider#useAsApiOnly()} called <em>after</em>
 * {@code Options.load} already installed the mappings.
 */
public interface ArmorHiderOptionsAccess {

    /**
     * Removes every Armor Hider key mapping from the options array and re-derives vanilla's lookup
     * maps. A no-op when none are present, so it is safe to call repeatedly.
     *
     * @return {@code true} when at least one mapping was actually removed.
     */
    boolean armorHider$removeKeyMappings();
}
