package de.zannagh.armorhider.api;

/**
 * Entrypoint interface for mods that want to integrate with Armor Hider.
 * <p>
 * Implement this interface and register it as an entrypoint in your {@code fabric.mod.json}:
 * <pre>
 * "entrypoints": {
 *   "armor-hider": ["com.example.mymod.MyArmorHiderIntegration"]
 * }
 * </pre>
 * <p>
 * When invoked, the {@link ArmorHiderApi} is fully initialized and can be accessed
 * via {@link ArmorHiderApi#getInstance()}.
 *
 * @since 0.12.0
 */
@FunctionalInterface
public interface ArmorHiderInitializer {
    String ENTRYPOINT_KEY = "armor-hider";

    void onInitializeArmorHider(ArmorHiderApi api);

    default int priority() {
        return 0;
    }
}
