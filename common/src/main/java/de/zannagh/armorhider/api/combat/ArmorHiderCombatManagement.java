package de.zannagh.armorhider.api.combat;

/**
 * @since 0.12.0
 */
public interface ArmorHiderCombatManagement {

    void overrideDefaultBehavior(ArmorHiderCombatManagement customManagement, int priority);
    
    /**
     * Registers a combat event consumer with custom behavior about whether combat events should be
     * processed and processed exclusively (cancelling default mod behavior).
     */
    void registerCombatEventConsumer(ArmorHiderCombatEventConsumer consumer);

    void registerCombatEvent(String playerDisplayName);
    
    void registerCombatEvent(String playerDisplayName, long timestamp);
    
    /**
     * Records a combat event for a specified player at a given timestamp.
     *
     * @param event The combat event to register ({@link ArmorHiderCombatEvent}).
     */
    void registerCombatEvent(ArmorHiderCombatEvent event);

    /**
     * Queries whether the player is currently in combat.
     * @param playerDisplayName The display name of the player.
     * @return true if the player is currently in combat.
     */
    boolean isInCombat(String playerDisplayName);

    /**
     * Calculates and returns the adjusted transparency for a player based on their combat status.
     *
     * @param playerDisplayName The display name of the player whose transparency is to be calculated.
     * @param originalTransparency The original transparency level before combat adjustments.
     * @return The adjusted transparency level based on the player's combat status.
     */
    double getCombatFade(String playerDisplayName, double originalTransparency);
}
