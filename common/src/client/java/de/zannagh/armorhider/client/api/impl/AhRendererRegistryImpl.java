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

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

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

    private static final HashMap<RenderScope, HashSet<Function<Pair<RenderScope, AhRenderer>, Boolean>>> SUPPRESSORS = new HashMap<>();
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
        AhRenderer renderer = EMPTY_RENDERER;
        if (scope == null || scope == RenderScope.NONE) {
            return renderer;
        }

        var list = RENDERERS.get(scope);
        if (list != null && !list.isEmpty()) {
            renderer = list.get(0).getSecond();
        }

        var fallback = RENDERERS.get(RenderScope.ALL);
        if (fallback != null && !fallback.isEmpty()) {
            renderer = fallback.get(0).getSecond();
        }

        if (SUPPRESSORS.containsKey(scope)) {
            var pair = Pair.of(scope, renderer);
            var set = SUPPRESSORS.get(scope);
            for (var evaluation : set) {
                if (evaluation.apply(pair)) {
                    return EMPTY_RENDERER;
                }
            }
        }

        return renderer;
    }

    public static <T extends AhRenderer> T getRenderer(Class<T> type) {
        if (type == null) return null;
        for (var registrations : RENDERERS.values()) {
            for (var renderPair : registrations) {
                var renderer = renderPair.getSecond();
                if (renderer.getClass() == type) {
                    return type.cast(renderer);
                }
            }
        }
        return null;
    }

    public static void suppressConditionally(RenderScope scope, Function<Pair<RenderScope, AhRenderer>, Boolean> evaluation) {
        if (SUPPRESSORS.containsKey(scope)) {
            var set = SUPPRESSORS.get(scope);
            set.add(evaluation);
        } else {
            var set = new HashSet<Function<Pair<RenderScope, AhRenderer>, Boolean>>();
            set.add(evaluation);
            SUPPRESSORS.put(scope, set);
        }
    }

    public static List<AhRenderer> getDefaultInterceptors() {
        return List.of(new ArmorHiderItemRenderer(), new ArmorHiderCapeRenderer(), new ArmorHiderElytraRenderer(), new ArmorHiderOffhandRenderer(), new ArmorHiderHeadRenderer());
    }
}
