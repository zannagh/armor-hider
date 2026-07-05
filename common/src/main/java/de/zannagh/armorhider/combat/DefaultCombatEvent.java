package de.zannagh.armorhider.combat;

import de.zannagh.armorhider.api.combat.ArmorHiderCombatEvent;
import org.jspecify.annotations.NonNull;

public class DefaultCombatEvent implements ArmorHiderCombatEvent {
    
    private final String playerDisplayName;
    private final long timeStamp;
    
    private long defaultFadeDurationSeconds = 10L;
    
    public DefaultCombatEvent(String playerDisplayName, long timeStamp){
        this.playerDisplayName = playerDisplayName;
        this.timeStamp = timeStamp;
    }
    
    @Override
    public @NonNull String getPlayerDisplayName() {
        return playerDisplayName;
    }

    @Override
    public @NonNull Long getTimestamp() {
        return timeStamp;
    }

    @Override
    public @NonNull Long getFadeDurationSeconds() {
        return defaultFadeDurationSeconds;
    }

    @Override
    public void setFadeDuration(long duration) {
        defaultFadeDurationSeconds = duration;
    }
}
