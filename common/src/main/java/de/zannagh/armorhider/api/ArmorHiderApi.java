package de.zannagh.armorhider.api;

import de.zannagh.armorhider.api.combat.ArmorHiderCombatManagementApi;

public interface ArmorHiderApi {

    /**
     * @since 0.12.0
     * @return The global API instance.
     * @throws IllegalStateException if the API has not been initialized yet.
     */
    static ArmorHiderApi getInstance() {
        return ArmorHiderApiImpl.getInstance();
    }

    /**
     * @since 0.12.0
     * @return An instance of {@link ArmorHiderCombatManagementApi} to register or change in-combat behavior.
     */
    ArmorHiderCombatManagementApi getCombatManagement();
}
