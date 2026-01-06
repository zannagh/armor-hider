package de.zannagh.armorhider.configuration;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ConfigurationItemSerializer implements TypeAdapterFactory {

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
                @SuppressWarnings("unchecked")
                Constructor<? extends ConfigurationItemBase<T>> constructor =
                        (Constructor<? extends ConfigurationItemBase<T>>) configClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                ConfigurationItemBase<T> instance = constructor.newInstance();
                instance.setValue(value);
                return instance;
            } catch (Exception e) {
                String exception = String.format("Could not instantiate ConfigurationItemBase of type %s. Make sure it has a constructor without arguments.", configClass.getName());
                throw new IOException(exception, e);
            }
        }
    }
}
