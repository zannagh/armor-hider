package de.zannagh.armorhider.api.combat;

import de.zannagh.armorhider.common.PrioritizedHandler;

/**
 * @since 0.12.0
 */
public interface ArmorHiderCombatEventConsumer extends PrioritizedHandler<ArmorHiderCombatEvent> {
    double getFadeFor(String playerDisplayName, double originalTransparency);

    boolean isPlayerConsideredInCombat(String playerDisplayName);
}
