package de.zannagh.armorhider.api;

import de.zannagh.armorhider.api.combat.ArmorHiderCombatManagementApi;
import de.zannagh.armorhider.combat.CombatManager;

public final class ArmorHiderApiImpl implements ArmorHiderApi {

    private static ArmorHiderApiImpl instance;

    private final CombatManager combatManager = new CombatManager();

    /**
     * @since 0.12.0
     * @return The global API instance.
     * @throws IllegalStateException if the API has not been initialized yet.
     */
    static ArmorHiderApiImpl getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ArmorHiderApi has not been initialized yet.");
        }
        return instance;
    }

    /**
     * @since 0.12.0
     * @throws IllegalStateException if the API has already been initialized.
     */
    public static void init() {
        if (instance != null) {
            throw new IllegalStateException("ArmorHiderApi has already been initialized.");
        }
        instance = new ArmorHiderApiImpl();
    }

    @Override
    public ArmorHiderCombatManagementApi getCombatManagement() {
        return combatManager;
    }

    public CombatManager getCombatManagerInternal() {
        return combatManager;
    }
}
