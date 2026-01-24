package de.zannagh.armorhider.configuration;

import de.zannagh.armorhider.configuration.items.implementations.*;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Centralized registry for ConfigurationItemBase factory methods.
 * Explicitly registers all implementations and creates both single-param and no-arg factories.
 */
public class ConfigurationItemFactoryRegistry {

    private static final ConcurrentHashMap<Class<?>, Function<Object, ConfigurationItemBase<?>>> VALUE_FACTORIES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Supplier<ConfigurationItemBase<?>>> DEFAULT_FACTORIES = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        // Explicitly register all ConfigurationItemBase implementations
        registerFactoriesForClass(ArmorOpacity.class);
        registerFactoriesForClass(CombatDetection.class);
        registerFactoriesForClass(OpacityAffectingElytraItem.class);
        registerFactoriesForClass(OpacityAffectingHatOrSkullItem.class);
        registerFactoriesForClass(PlayerName.class);
        registerFactoriesForClass(PlayerUuid.class);

        initialized = true;
    }

    public static Function<Object, ConfigurationItemBase<?>> getValueFactory(Class<?> clazz) {
        if (!initialized) {
            initialize();
        }
        return VALUE_FACTORIES.get(clazz);
    }

    public static Supplier<ConfigurationItemBase<?>> getDefaultFactory(Class<?> clazz) {
        if (!initialized) {
            initialize();
        }
        return DEFAULT_FACTORIES.get(clazz);
    }

    private static void registerFactoriesForClass(Class<? extends ConfigurationItemBase<?>> clazz) {
        try {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            Constructor<?> singleParamConstructor = null;
            Constructor<?> noArgConstructor = null;

            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == 1) {
                    singleParamConstructor = constructor;
                } else if (constructor.getParameterCount() == 0) {
                    noArgConstructor = constructor;
                }
            }

            if (singleParamConstructor == null) {
                throw new IllegalStateException(
                    "ConfigurationItemBase implementation " + clazz.getName() +
                    " must have a constructor that takes exactly one parameter (the value).");
            }

            Constructor<?> finalSingleParamConstructor = singleParamConstructor;
            finalSingleParamConstructor.setAccessible(true);
            Function<Object, ConfigurationItemBase<?>> valueFactory = value -> {
                try {
                    return (ConfigurationItemBase<?>) finalSingleParamConstructor.newInstance(value);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
                }
            };
            VALUE_FACTORIES.put(clazz, valueFactory);

            Supplier<ConfigurationItemBase<?>> defaultFactory;
            if (noArgConstructor != null) {
                defaultFactory = getConfigurationItemBaseSupplier(clazz, noArgConstructor);
            } else {
                defaultFactory = () -> {
                    try {
                        return (ConfigurationItemBase<?>) finalSingleParamConstructor.newInstance(new Object[]{null});
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
                    }
                };
            }
            DEFAULT_FACTORIES.put(clazz, defaultFactory);

        } catch (Exception e) {
            throw new RuntimeException("Failed to register factories for " + clazz.getName(), e);
        }
    }

    private static Supplier<ConfigurationItemBase<?>> getConfigurationItemBaseSupplier(Class<? extends ConfigurationItemBase<?>> clazz, Constructor<?> noArgConstructor) {
        noArgConstructor.setAccessible(true);
        return () -> {
            try {
                return (ConfigurationItemBase<?>) noArgConstructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
            }
        };
    }
}
