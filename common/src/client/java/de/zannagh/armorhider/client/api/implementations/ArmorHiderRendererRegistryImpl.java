package de.zannagh.armorhider.client.api.implementations;

import de.zannagh.armorhider.client.api.ArmorHiderRenderInterceptionRegistry;
import de.zannagh.armorhider.client.api.ArmorHiderRenderer;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.RenderScopeProvider;

import java.util.HashMap;
import java.util.Map;

public class ArmorHiderRendererRegistryImpl implements ArmorHiderRenderInterceptionRegistry {

    private final Map<RenderScope, Map<Integer, ArmorHiderRenderer>> renderers = new HashMap<>();

    @Override
    public void register(ArmorHiderRenderer renderer, int priority) {
        // TODO: reigster the renderer with the assigned priority, reorder the map.
    }

    @Override
    public void unregister(ArmorHiderRenderer renderer, int priority) {
       // TODO: unregister the matching renderer, priority pair.
    }

    @Override
    public ArmorHiderRenderer getRenderer(RenderScope scope) {
        // TODO: return first of matching scope.
        return null;
    }
}
