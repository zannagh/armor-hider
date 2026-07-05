package de.zannagh.armorhider.api;

import de.zannagh.armorhider.ArmorHider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Entrypoint interface for mods that want to integrate with Armor Hider.
 * <p>
 * Implement this interface and register it through Java's standard {@link ServiceLoader}
 * mechanism so it works on every loader (Fabric, NeoForge, …) without per-loader
 * configuration. Add a resource file
 * <pre>
 * META-INF/services/de.zannagh.armorhider.api.ArmorHiderInitializer
 * </pre>
 * to your mod jar, containing the fully-qualified name of each implementation, one per line:
 * <pre>
 * com.example.mymod.MyArmorHiderIntegration
 * </pre>
 * <p>
 * When invoked, the {@link ArmorHiderApi} is fully initialized and can be accessed
 * via {@link ArmorHiderApi#getInstance()}.
 *
 * @since 0.12.0
 */
@FunctionalInterface
public interface ArmorHiderInitializer {

    void onInitializeArmorHider(ArmorHiderApi api);

    /**
     * Initialization order across registered initializers — lower values run first
     * (MC-modding convention, matching
     * {@link de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi#defaultPriority}).
     * <p>
     * Default {@code 0}; ties resolve in {@link ServiceLoader} discovery order.
     */
    default int priority() {
        return 0;
    }

    /**
     * Loader-agnostic dispatcher: discovers all registered {@link ArmorHiderInitializer}s
     * via {@link ServiceLoader}, sorts them by {@link #priority()} (lower first), and
     * invokes {@link #onInitializeArmorHider(ArmorHiderApi)} on each. A throw from one
     * initializer is logged and does not block the rest.
     * <p>
     * Called from {@link de.zannagh.armorhider.ArmorHider#init()}; integrators should not
     * call this themselves.
     */
    static void dispatchAll(ArmorHiderApi api) {
        List<ArmorHiderInitializer> initializers = new ArrayList<>();
        ServiceLoader<ArmorHiderInitializer> loader = ServiceLoader.load(
                ArmorHiderInitializer.class, ArmorHiderInitializer.class.getClassLoader());
        Iterator<ArmorHiderInitializer> it = loader.iterator();
        while (it.hasNext()) {
            try {
                initializers.add(it.next());
            } catch (ServiceConfigurationError | RuntimeException e) {
                ArmorHider.LOGGER.error("Skipping malformed ArmorHiderInitializer service entry", e);
            }
        }
        initializers.sort(Comparator.comparingInt(ArmorHiderInitializer::priority));
        for (ArmorHiderInitializer initializer : initializers) {
            try {
                initializer.onInitializeArmorHider(api);
            } catch (RuntimeException e) {
                ArmorHider.LOGGER.error("ArmorHiderInitializer {} threw during onInitializeArmorHider",
                        initializer.getClass().getName(), e);
            }
        }
    }
}
