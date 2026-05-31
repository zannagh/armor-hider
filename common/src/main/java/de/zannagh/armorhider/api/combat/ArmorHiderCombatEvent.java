package de.zannagh.armorhider.api.combat;

import org.jspecify.annotations.NonNull;

/**
 * Represents a combat event with player display name, timestamp, and fade duration.
 * 
 * @since 0.12.0
 */
public interface ArmorHiderCombatEvent {
    @NonNull String getPlayerDisplayName();
    
    @NonNull Long getTimestamp();
    
    @NonNull Long getFadeDurationSeconds();
    
    void setFadeDuration(long duration);
}
