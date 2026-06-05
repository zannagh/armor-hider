package de.zannagh.armorhider.client.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The ArmorHiderClientCombatApi interface provides methods to handle combat-related
 * events and determine if combat logging is required for specific players. It is 
 * intended to be implemented by classes that manage client-side combat logic.
 * 
 * @since 0.12.0
 */
public interface ArmorHiderClientCombatApi {
    /**
     * Handles combat logic, such as updating combat-related statuses, when an entity is attacked.
     *
     * @param damageSource The source of the damage, which provides details about the nature of the attack.
     * @param victim The player who is the victim of the attack, or null if the victim is not a player.
     *               
     * @since 0.12.0
     */
    void handleCombat(DamageSource damageSource, @Nullable Player victim);

    /**
     * Determines if combat logging should be enabled for a specific player.
     * The decision depends on server-wide combat detection settings or
     * the individual player's configuration if server settings are not enforced.
     *
     * @param player The player entity for which to determine combat logging behavior.
     * @return true if combat logging should be enabled for the specified player, false otherwise.
     * 
     * @since 0.12.0
     */
    boolean shouldLogCombatForPlayer(Player player);
}
