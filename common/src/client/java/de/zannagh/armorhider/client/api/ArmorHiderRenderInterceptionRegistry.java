package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.RenderScopeProvider;

public interface ArmorHiderRenderInterceptionRegistry {
    void register(ArmorHiderRenderer renderer, int priority);

    void unregister(ArmorHiderRenderer renderer, int priority);

    ArmorHiderRenderer getRenderer(RenderScope scope);
}
