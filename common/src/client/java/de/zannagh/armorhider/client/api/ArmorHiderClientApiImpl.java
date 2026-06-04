package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.client.api.combat.ArmorHiderClientCombatApi;
import de.zannagh.armorhider.client.api.implementations.AhRenderInterceptionApiImpl;
import de.zannagh.armorhider.client.api.render.AhRenderInterceptionApi;
import de.zannagh.armorhider.client.api.render.ArmorHiderRenderingScopeApi;
import de.zannagh.armorhider.client.api.implementations.ArmorHiderRenderingScopeApiImpl;
import de.zannagh.armorhider.client.combat.ClientCombatManager;

public final class ArmorHiderClientApiImpl implements ArmorHiderClientApi {

    private static ArmorHiderClientApiImpl instance;

    private final ClientCombatManager clientCombatManager = new ClientCombatManager();

    private final ArmorHiderRenderingScopeApiImpl renderingScopeApi = new ArmorHiderRenderingScopeApiImpl();

    private final AhRenderInterceptionApi renderInterceptionApi = new AhRenderInterceptionApiImpl();

    static ArmorHiderClientApiImpl getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ArmorHiderClientApi has not been initialized yet.");
        }
        return instance;
    }

    /**
     * @since 0.12.0
     * @throws IllegalStateException if the client API has already been initialized.
     */
    public static void init() {
        if (instance != null) {
            throw new IllegalStateException("ArmorHiderClientApi has already been initialized.");
        }
        instance = new ArmorHiderClientApiImpl();
    }

    @Override
    public ArmorHiderClientCombatApi getClientCombatApi() {
        return clientCombatManager;
    }

    @Override
    public ArmorHiderRenderingScopeApi getRenderingScopeApi() {
        return renderingScopeApi;
    }

    @Override
    public AhRenderInterceptionApi getRenderApi() {
        return renderInterceptionApi;
    }
}
