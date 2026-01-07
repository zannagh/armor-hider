package de.zannagh.armorhider.configuration;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.zannagh.armorhider.ArmorHider;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Custom Gson serializer for ConfigurationSource classes.
 * Automatically initializes null ConfigurationItemBase fields after deserialization.
 * Additionally, sets a flag if a mismatch between declaration and serialized content is detected.
 */
public class ConfigurationSourceSerializer implements TypeAdapterFactory {

    private static final HashMap<Class<?>, Supplier<ConfigurationItemBase<?>>> fieldFactories = new HashMap<>();
    private static boolean fieldFactoriesRegistered = false;

    public ConfigurationSourceSerializer() {
        if (!fieldFactoriesRegistered) {
            List<Class<? extends ConfigurationItemBase<?>>> implementations = findAllImplementations();

            for (Class<? extends ConfigurationItemBase<?>> clazz : implementations) {
                registerFactoryForClass(clazz);
            }

            fieldFactoriesRegistered = true;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends ConfigurationItemBase<?>>> findAllImplementations() {
        String packageName = "de.zannagh.armorhider.configuration.items.implementations";
        List<Class<? extends ConfigurationItemBase<?>>> classes = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if (resource.getProtocol().equals("file")) {
                    File directory = new File(resource.toURI());
                    classes.addAll(findClassesInDirectory(directory, packageName));
                } else if (resource.getProtocol().equals("jar")) {
                    String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                    classes.addAll(findClassesInJar(jarPath, path));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan for ConfigurationItemBase implementations", e);
        }

        return classes;
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends ConfigurationItemBase<?>>> findClassesInDirectory(File directory, String packageName) {
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
    private List<Class<? extends ConfigurationItemBase<?>>> findClassesInJar(String jarPath, String path) {
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

    private boolean isValidImplementation(Class<?> clazz) {
        return ConfigurationItemBase.class.isAssignableFrom(clazz) &&
                !clazz.equals(ConfigurationItemBase.class) &&
                !Modifier.isAbstract(clazz.getModifiers()) &&
                !clazz.isInterface();
    }

    private void registerFactoryForClass(Class<? extends ConfigurationItemBase<?>> clazz) {
        try {
            Constructor<? extends ConfigurationItemBase<?>> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Supplier<ConfigurationItemBase<?>> factory = () -> {
                try {
                    return constructor.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
                }
            };
            fieldFactories.put(clazz, factory);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No no-arg constructor found for " + clazz.getName(), e);
        }
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class<?> rawType = typeToken.getRawType();

        if (!ConfigurationSource.class.isAssignableFrom(rawType)) {
            return null;
        }

        TypeAdapter<T> defaultAdapter = gson.getDelegateAdapter(this, typeToken);

        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                defaultAdapter.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws JsonParseException {
                JsonElement jsonElement = Streams.parse(in);

                T instance = defaultAdapter.fromJsonTree(jsonElement);

                if (instance != null) {
                    boolean hasChanged = false;

                    if (jsonElement.isJsonObject()) {
                        hasChanged = checkJsonForMissingFields(instance, jsonElement.getAsJsonObject());
                    }

                    if (initializeNullConfigFields(instance)) {
                        hasChanged = true;
                    }

                    if (hasChanged && instance instanceof ConfigurationSource<?> configurationSource) {
                        configurationSource.setHasChangedFromSerializedContent();
                    }
                }
                return instance;
            }
        };
    }

    private boolean checkJsonForMissingFields(@NonNull Object config, @NonNull JsonObject jsonObject) {
        for (Field field : config.getClass().getDeclaredFields()) {
            if (ConfigurationItemBase.class.isAssignableFrom(field.getType())) {
                Set<String> fieldNames = getSerializedFieldNames(field);

                boolean foundInJson = false;
                for (String name : fieldNames) {
                    if (jsonObject.has(name)) {
                        foundInJson = true;
                        break;
                    }
                }

                if (!foundInJson) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> getSerializedFieldNames(Field field) {
        Set<String> names = new HashSet<>();

        SerializedName annotation = field.getAnnotation(SerializedName.class);
        if (annotation != null) {
            names.add(annotation.value());
            Collections.addAll(names, annotation.alternate());
        } else {
            names.add(field.getName());
        }

        return names;
    }

    private boolean initializeNullConfigFields(@NonNull Object config) {
        boolean hasChangedComparedToSerializedContent = false;
        for (Field field : config.getClass().getDeclaredFields()) {
            if (ConfigurationItemBase.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(config) == null) {
                        Object instance;
                        if (fieldFactories.containsKey(field.getType())) {
                            instance = fieldFactories.get(field.getType()).get();
                        } else {
                            instance = field.getType().getDeclaredConstructor().newInstance();
                        }
                        field.set(config, instance);
                        hasChangedComparedToSerializedContent = true;
                    }
                } catch (Exception e) {
                    ArmorHider.LOGGER.error("Failed to initialize configuration field: {}", field.getName(), e);
                }
            }
        }
        return hasChangedComparedToSerializedContent;
    }
}
