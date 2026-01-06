package de.zannagh.armorhider.configuration;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.zannagh.armorhider.ArmorHider;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Custom Gson serializer for ConfigurationSource classes.
 * Automatically initializes null ConfigurationItemBase fields after deserialization.
 */
public class ConfigurationSourceSerializer implements TypeAdapterFactory {

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
            public T read(JsonReader in) throws IOException {
                T instance = defaultAdapter.read(in);
                if (instance != null) {
                    var hasChanged = initializeNullConfigFields(instance);
                    if (hasChanged && instance instanceof ConfigurationSource configurationSource) {
                        configurationSource.setHasChangedFromSerializedContent();
                    }
                }
                return instance;
            }
        };
    }

    /**
     * Initializes any null ConfigurationItemBase fields using reflection.
     */
    private boolean initializeNullConfigFields(@NonNull Object config) {
        boolean hasChangedComparedToSerializedContent = false;
        for (Field field : config.getClass().getDeclaredFields()) {
            if (ConfigurationItemBase.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(config) == null) {
                        Object instance = field.getType().getDeclaredConstructor().newInstance();
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
