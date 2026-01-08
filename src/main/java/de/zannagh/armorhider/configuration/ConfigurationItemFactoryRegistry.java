package de.zannagh.armorhider.configuration;

import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Centralized registry for ConfigurationItemBase factory methods.
 * Scans the implementations package once and creates both single-param and no-arg factories.
 */
public class ConfigurationItemFactoryRegistry {

    private static final List<String> IMPLEMENTATIONS_PACKAGES = List.of("de.zannagh.armorhider.configuration.items.implementations");
    private static final ConcurrentHashMap<Class<?>, Function<Object, ConfigurationItemBase<?>>> VALUE_FACTORIES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Supplier<ConfigurationItemBase<?>>> DEFAULT_FACTORIES = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        List<Class<? extends ConfigurationItemBase<?>>> implementations = scanForImplementations();
        for (Class<? extends ConfigurationItemBase<?>> clazz : implementations) {
            registerFactoriesForClass(clazz);
        }

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

    private static @NonNull Supplier<ConfigurationItemBase<?>> getConfigurationItemBaseSupplier(Class<? extends ConfigurationItemBase<?>> clazz, Constructor<?> noArgConstructor) {
        noArgConstructor.setAccessible(true);
        return () -> {
            try {
                return (ConfigurationItemBase<?>) noArgConstructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
            }
        };
    }

    private static List<Class<? extends ConfigurationItemBase<?>>> scanForImplementations() {
        List<Class<? extends ConfigurationItemBase<?>>> classes = new ArrayList<>();

        try {
            for (String packageName : IMPLEMENTATIONS_PACKAGES) {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                String path = packageName.replace('.', '/');
                Enumeration<URL> resources = classLoader.getResources(path);

                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();

                    if (resource.getProtocol().equals("file")) {
                        File directory = new File(resource.toURI());
                        classes.addAll(findClassesInDirectory(directory, packageName));
                    } else if (resource.getProtocol().equals("jar")) {
                        String rawPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                        String jarPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
                        classes.addAll(findClassesInJar(jarPath, path));
                    }
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan for ConfigurationItemBase implementations", e);
        }

        return classes;
    }

    @SuppressWarnings("unchecked")
    private static List<Class<? extends ConfigurationItemBase<?>>> findClassesInDirectory(File directory, String packageName) {
        List<Class<? extends ConfigurationItemBase<?>>> classes = new ArrayList<>();

        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (isValidImplementation(clazz)) {
                        classes.add((Class<? extends ConfigurationItemBase<?>>) clazz);
                    }
                } catch (ClassNotFoundException e) {
                    // Skip classes that can't be loaded
                }
            }
        }

        return classes;
    }

    @SuppressWarnings("unchecked")
    private static List<Class<? extends ConfigurationItemBase<?>>> findClassesInJar(String jarPath, String path) {
        List<Class<? extends ConfigurationItemBase<?>>> classes = new ArrayList<>();

        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith(path) && name.endsWith(".class")) {
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (isValidImplementation(clazz)) {
                            classes.add((Class<? extends ConfigurationItemBase<?>>) clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        // Skip classes that can't be loaded
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JAR file: " + jarPath, e);
        }

        return classes;
    }

    private static boolean isValidImplementation(Class<?> clazz) {
        return ConfigurationItemBase.class.isAssignableFrom(clazz) &&
                !clazz.equals(ConfigurationItemBase.class) &&
                !Modifier.isAbstract(clazz.getModifiers()) &&
                !clazz.isInterface();
    }
}
