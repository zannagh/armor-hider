package de.zannagh.armorhider.configuration;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ConfigurationItemSerializer implements TypeAdapterFactory {

    private static boolean typeFactoriesRegistered = false;

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Type type = typeToken.getType();
        Class<?> rawType;

        if (type instanceof ParameterizedType parameterizedType) {
            if (!(parameterizedType.getRawType() instanceof Class<?>)) {
                return null;
            }
            rawType = (Class<?>) parameterizedType.getRawType();
        } else if (type instanceof Class<?>) {
            rawType = (Class<?>) type;
        } else {
            return null;
        }

        if (!ConfigurationItemBase.class.isAssignableFrom(rawType)) {
            return null;
        }

        Type valueType = getTypeParameter(rawType);
        if (valueType == null) {
            return null;
        }

        TypeAdapter<?> valueAdapter = gson.getAdapter(TypeToken.get(valueType));

        @SuppressWarnings({"rawtypes", "unchecked"})
        TypeAdapter<T> adapter = new ConfigurationItemTypeAdapter(rawType, valueAdapter);
        return adapter;
    }

    private Type getTypeParameter(Class<?> implementation) {
        Type genericSuperclass = implementation.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType parameterized) {
            Type rawType = parameterized.getRawType();
            if (rawType instanceof Class<?> superClass && ConfigurationItemBase.class.isAssignableFrom(superClass)) {
                return parameterized.getActualTypeArguments()[0];
            }
        }

        if (genericSuperclass instanceof Class<?> superClass && !superClass.equals(Object.class)) {
            return getTypeParameter(superClass);
        }

        return null;
    }
    
    public ConfigurationItemSerializer() {
        if (!typeFactoriesRegistered) {
            List<Class<? extends ConfigurationItemBase<?>>> implementations = findAllImplementations();

            for (Class<? extends ConfigurationItemBase<?>> clazz : implementations) {
                registerFactoryForClass(clazz);
            }

            typeFactoriesRegistered = true;
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

    @SuppressWarnings("unchecked")
    private <T> void registerFactoryForClass(Class<? extends ConfigurationItemBase<?>> clazz) {
        try {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            Constructor<?> singleParamConstructor = null;
            Constructor<?> noArgConstructor = null;

            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == 1) {
                    singleParamConstructor = constructor;
                    break;
                } else if (constructor.getParameterCount() == 0) {
                    noArgConstructor = constructor;
                }
            }

            if (singleParamConstructor != null) {
                Constructor<?> finalConstructor = singleParamConstructor;
                finalConstructor.setAccessible(true);
                Function<Object, ConfigurationItemBase<?>> factory = value -> {
                    try {
                        return (ConfigurationItemBase<?>) finalConstructor.newInstance(value);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
                    }
                };
                ConfigurationItemTypeAdapter.typeFactories.put(clazz, factory);
            } else if (noArgConstructor != null) {
                Constructor<?> finalConstructor = noArgConstructor;
                finalConstructor.setAccessible(true);
                Function<Object, ConfigurationItemBase<?>> factory = value -> {
                    try {
                        ConfigurationItemBase instance = (ConfigurationItemBase) finalConstructor.newInstance();
                        instance.setValue(value);
                        return instance;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
                    }
                };
                ConfigurationItemTypeAdapter.typeFactories.put(clazz, factory);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register factory for " + clazz.getName(), e);
        }
    }

    public static <T> void registerTypeFactory(Class<? extends ConfigurationItemBase<T>> configClass, Function<T, ConfigurationItemBase<T>> factory) {
        ConfigurationItemTypeAdapter.registerFactory(configClass, factory);
    }

    private static class ConfigurationItemTypeAdapter<T> extends TypeAdapter<ConfigurationItemBase<T>> {
        private final Class<? extends ConfigurationItemBase<?>> configClass;
        private final TypeAdapter<T> valueAdapter;

        private static final HashMap<Class<?>, Function<Object, ConfigurationItemBase<?>>> typeFactories = new HashMap<>();

        public static <V> void registerFactory(Class<? extends ConfigurationItemBase<V>> configClass, Function<V, ConfigurationItemBase<V>> factory) {
            @SuppressWarnings("unchecked")
            Function<Object, ConfigurationItemBase<?>> genericFactory = (Function<Object, ConfigurationItemBase<?>>) (Function<?, ?>) factory;
            typeFactories.put(configClass, genericFactory);
        }

        public ConfigurationItemTypeAdapter(
                Class<? extends ConfigurationItemBase<?>> configClass,
                TypeAdapter<T> valueAdapter) {
            this.configClass = configClass;
            this.valueAdapter = valueAdapter;
        }

        @Override
        public void write(JsonWriter out, ConfigurationItemBase<T> value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            valueAdapter.write(out, value.getValue());
        }

        @Override
        public ConfigurationItemBase<T> read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            T value = valueAdapter.read(in);

            try {
                if (typeFactories.containsKey(configClass)) {
                    @SuppressWarnings("unchecked")
                    Function<Object, ConfigurationItemBase<T>> factory =
                            (Function<Object, ConfigurationItemBase<T>>) (Function<?, ?>) typeFactories.get(configClass);
                    return factory.apply(value);
                }
                throw new IllegalArgumentException(String.format("No factory registered for ConfigurationItemBase of type %s", configClass.getName()));
            } catch (Exception e) {
                String exception = String.format("Could not instantiate ConfigurationItemBase of type %s. Make sure a factory was registered for it. %s", configClass.getName(), e.getMessage());
                throw new IOException(exception, e);
            }
        }
    }
}
