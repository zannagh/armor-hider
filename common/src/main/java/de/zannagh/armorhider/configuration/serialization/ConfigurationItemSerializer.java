package de.zannagh.armorhider.configuration.serialization;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.zannagh.armorhider.configuration.ConfigurationItemFactoryRegistry;
import de.zannagh.armorhider.configuration.abstractions.ConfigurationItemBase;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ConfigurationItemSerializer implements TypeAdapterFactory {

    public ConfigurationItemSerializer() {
        // Initialize the factory registry on first instantiation
        ConfigurationItemFactoryRegistry.initialize();
    }

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

    /**
     * Resolves the actual {@code value} type of a {@link ConfigurationItemBase} implementation, i.e. the
     * concrete type argument of {@code ConfigurationItemBase<V>} for the given class, resolving type
     * variables through the whole superclass chain.
     *
     * <p>We deliberately resolve against {@code ConfigurationItemBase} itself rather than the first
     * parameterized superclass we encounter. That distinction matters for map-backed items: for example
     * {@code HashMapConfigItem<T> extends ConfigurationItemBase<HashMap<String, T>>}, so a subclass such as
     * {@code ServerMappedIndividualConfigurations extends HashMapConfigItem<IndividualConfigurations>} has a
     * value type of {@code HashMap<String, IndividualConfigurations>} — NOT {@code IndividualConfigurations}.
     * Returning the latter would build the wrong value adapter and break (de)serialization of nested maps.
     */
    private Type getTypeParameter(Class<?> implementation) {
        Map<TypeVariable<?>, Type> substitutions = new HashMap<>();
        Class<?> raw = implementation;
        while (raw != null && raw != Object.class) {
            Type genericSuperclass = raw.getGenericSuperclass();
            if (genericSuperclass instanceof Class<?> plainSuperclass) {
                // A non-parameterized superclass (e.g. BooleanConfigItem) contributes no substitutions.
                raw = plainSuperclass;
                continue;
            }
            if (!(genericSuperclass instanceof ParameterizedType parameterized)
                    || !(parameterized.getRawType() instanceof Class<?> superRaw)) {
                return null;
            }

            TypeVariable<?>[] variables = superRaw.getTypeParameters();
            Type[] arguments = parameterized.getActualTypeArguments();
            Map<TypeVariable<?>, Type> resolvedForSuperclass = new HashMap<>();
            for (int i = 0; i < variables.length; i++) {
                resolvedForSuperclass.put(variables[i], resolveTypeVariables(arguments[i], substitutions));
            }

            if (superRaw == ConfigurationItemBase.class) {
                return resolvedForSuperclass.get(variables[0]);
            }

            substitutions = resolvedForSuperclass;
            raw = superRaw;
        }

        return null;
    }

    /** Substitutes any type variables in {@code type} using the supplied mapping, recursing into generics. */
    private Type resolveTypeVariables(Type type, Map<TypeVariable<?>, Type> substitutions) {
        if (type instanceof TypeVariable<?> variable) {
            Type mapped = substitutions.get(variable);
            return mapped != null ? mapped : variable;
        }
        if (type instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();
            Type[] resolved = new Type[arguments.length];
            boolean changed = false;
            for (int i = 0; i < arguments.length; i++) {
                resolved[i] = resolveTypeVariables(arguments[i], substitutions);
                if (resolved[i] != arguments[i]) {
                    changed = true;
                }
            }
            if (!changed) {
                return parameterized;
            }
            return TypeToken.getParameterized(parameterized.getRawType(), resolved).getType();
        }
        return type;
    }

    private static class ConfigurationItemTypeAdapter<T> extends TypeAdapter<ConfigurationItemBase<T>> {
        private final Class<? extends ConfigurationItemBase<?>> configClass;
        private final TypeAdapter<T> valueAdapter;
        private final Function<Object, ConfigurationItemBase<T>> cachedFactory;

        @SuppressWarnings("unchecked")
        public ConfigurationItemTypeAdapter(
                Class<? extends ConfigurationItemBase<?>> configClass,
                TypeAdapter<T> valueAdapter) {
            this.configClass = configClass;
            this.valueAdapter = valueAdapter;

            // Get factory from centralized registry and cache it
            Function<Object, ConfigurationItemBase<?>> factory = ConfigurationItemFactoryRegistry.getValueFactory(configClass);
            if (factory == null) {
                throw new IllegalStateException("No factory registered for " + configClass.getName());
            }
            this.cachedFactory = (Function<Object, ConfigurationItemBase<T>>) (Function<?, ?>) factory;
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

            // Use cached factory - no HashMap lookup, no type casting, direct invocation
            try {
                return cachedFactory.apply(value);
            } catch (RuntimeException e) {
                throw new IOException("Failed to instantiate " + configClass.getName() + ": " + e.getMessage(), e);
            }
        }
    }
}
