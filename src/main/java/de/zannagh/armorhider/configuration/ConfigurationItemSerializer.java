package de.zannagh.armorhider.configuration;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
