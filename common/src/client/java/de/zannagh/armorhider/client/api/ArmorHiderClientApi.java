package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.client.api.combat.ArmorHiderClientCombatApi;

/**
 * @since 0.12.0
 */
public interface ArmorHiderClientApi {

    /**
     * @since 0.12.0
     * @return The global client API instance.
     * @throws IllegalStateException if the client API has not been initialized yet.
     */
    static ArmorHiderClientApi getInstance() {
        return ArmorHiderClientApiImpl.getInstance();
    }

    /**
     * @since 0.12.0
     * @return An instance of {@link ArmorHiderClientCombatApi} for client-side combat handling.
     */
    ArmorHiderClientCombatApi getClientCombatApi();
}
