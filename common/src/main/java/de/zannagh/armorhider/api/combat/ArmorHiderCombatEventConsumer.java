package de.zannagh.armorhider.api.combat;

/**
 * @since 0.12.0
 */
public interface ArmorHiderCombatEventConsumer {
    
    int getPriority();
    
    boolean shouldConsume(ArmorHiderCombatEvent event);
    
    boolean shouldConsumeExclusively(ArmorHiderCombatEvent event);

    /**
     * Applies the consumer to the event. The consumer can decide internally to change fadeoff time.
     * @param event The event to apply the consumer to.
     * @return The modified event after applying the consumer.
     */
    ArmorHiderCombatEvent apply(ArmorHiderCombatEvent event);
}
