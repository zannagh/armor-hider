package de.zannagh.armorhider.client.api.implementations;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.client.api.ArmorHiderRenderInterceptionRegistry;
import de.zannagh.armorhider.client.api.ArmorHiderRenderer;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.RenderScopeProvider;

import java.util.*;

public class ArmorHiderRendererRegistryImpl implements ArmorHiderRenderInterceptionRegistry {

    private final Map<RenderScope, ArrayList<Pair<Integer, ArmorHiderRenderer>>> renderers = new HashMap<>();

    private final ArmorHiderRenderer emptyRenderer = new ArmorHiderEmptyRenderer();
    @Override
    public void register(ArmorHiderRenderer renderer, int priority) {
        var targetScope = renderer.getTargetScope();
        if (!renderers.containsKey(targetScope)) {
            var newSet = new ArrayList<Pair<Integer, ArmorHiderRenderer>>();
            newSet.add(Pair.of(priority, renderer));
            renderers.put(targetScope, newSet);
            return;
        }
        var set = renderers.get(targetScope);
        set.add(Pair.of(priority, renderer));
        renderers.get(targetScope).sort(Comparator.comparingInt(Pair::getFirst));
    }

    @Override
    public void unregister(ArmorHiderRenderer renderer, int priority) {
       // TODO: unregister the matching renderer, priority pair.
    }

    @Override
    public ArmorHiderRenderer getRenderer(RenderScope scope) {
        var set = renderers.get(scope);
        if (set == null) {
            if (renderers.containsKey(RenderScope.ALL)) {
                return getRenderer(RenderScope.ALL);
            }
            return emptyRenderer;
        }
        return set.get(0).getSecond();
    }

    public void registerDefaultInterceptors(){
        register(new ArmorHiderItemRenderer(), this.defaultPriority());
    }
}
