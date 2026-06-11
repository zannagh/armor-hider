package de.zannagh.armorhider.client.api.impl;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.client.api.AhRenderer;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.interceptors.ArmorHiderCapeRenderer;
import de.zannagh.armorhider.client.render.interceptors.ArmorHiderElytraRenderer;
import de.zannagh.armorhider.client.render.interceptors.ArmorHiderEmptyRenderer;
import de.zannagh.armorhider.client.render.interceptors.ArmorHiderHeadRenderer;
import de.zannagh.armorhider.client.render.interceptors.ArmorHiderItemRenderer;
import de.zannagh.armorhider.client.render.interceptors.ArmorHiderOffhandRenderer;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Objects;

/**
 * Storage and lookup for {@link AhRenderer}s. Not part of the public API; reached through the
 * static methods on {@link de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi}.
 * <p>
 * Priority semantics and {@link RenderScope#ALL} fallback are documented on the API interface.
 */
@ApiStatus.Internal
public final class AhRendererRegistryImpl {

    public static final int DEFAULT_PRIORITY = 1000;

    private static final EnumMap<RenderScope, ArrayList<Pair<Integer, AhRenderer>>> RENDERERS =
            new EnumMap<>(RenderScope.class);

    private static final AhRenderer EMPTY_RENDERER = new ArmorHiderEmptyRenderer();

    private AhRendererRegistryImpl() {}

    public static void register(AhRenderer renderer, int priority) {
        var list = RENDERERS.computeIfAbsent(renderer.getTargetScope(), k -> new ArrayList<>());
        list.add(Pair.of(priority, renderer));
        list.sort(Comparator.comparingInt(Pair::getFirst));
    }

    public static void unregister(AhRenderer renderer, int priority) {
        var list = RENDERERS.get(renderer.getTargetScope());
        if (list == null) return;
        list.removeIf(p -> p.getFirst() == priority && Objects.equals(p.getSecond(), renderer));
        if (list.isEmpty()) {
            RENDERERS.remove(renderer.getTargetScope());
        }
    }

    public static AhRenderer getRenderer(RenderScope scope) {
        var list = RENDERERS.get(scope);
        if (list != null && !list.isEmpty()) {
            return list.get(0).getSecond();
        }
        var fallback = RENDERERS.get(RenderScope.ALL);
        if (fallback != null && !fallback.isEmpty()) {
            return fallback.get(0).getSecond();
        }
        return EMPTY_RENDERER;
    }

    public static void registerDefaultInterceptors() {
        register(new ArmorHiderItemRenderer(), DEFAULT_PRIORITY);
        register(new ArmorHiderCapeRenderer(), DEFAULT_PRIORITY);
        register(new ArmorHiderElytraRenderer(), DEFAULT_PRIORITY);
        register(new ArmorHiderOffhandRenderer(), DEFAULT_PRIORITY);
        register(new ArmorHiderHeadRenderer(), DEFAULT_PRIORITY);
    }
}
