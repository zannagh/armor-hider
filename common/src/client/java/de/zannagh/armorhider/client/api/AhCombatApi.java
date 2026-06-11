package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.client.api.impl.AhCombatApiImpl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Static entry point for client-side combat detection.
 * <p>
 * Armor Hider can re-show armor (revert to the vanilla model) while a player is "in combat" — used
 * by the {@code inCombatUseDefaultModel} player setting. Whether a damage event counts as combat is
 * driven by per-player config and (optionally) a server-side override; the implementation here
 * registers combat events with the shared combat manager and forwards them to the server when
 * appropriate.
 *
 * @since 0.12.0
 */
@ApiStatus.NonExtendable
public interface AhCombatApi {

    /**
     * Handle a damage event for combat tracking. Registers a combat event for both the victim and
     * the attacker (if either is a player whose config opts into combat detection) and broadcasts
     * the event to the server.
     *
     * @param damageSource the source of the damage — used to find the attacker.
     * @param victim       the player who was hit, or {@code null} if the victim is not a player.
     */
    static void handleCombat(DamageSource damageSource, @Nullable Player victim) {
        AhCombatApiImpl.handleCombat(damageSource, victim);
    }

    /**
     * Whether combat events should be logged for the given player. Server-wide combat detection
     * wins if enabled; otherwise the player's resolved per-player config decides.
     */
    static boolean shouldLogCombatForPlayer(Player player) {
        return AhCombatApiImpl.shouldLogCombatForPlayer(player);
    }
}
