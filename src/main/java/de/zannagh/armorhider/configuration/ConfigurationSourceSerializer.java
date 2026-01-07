package de.zannagh.armorhider.configuration;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.zannagh.armorhider.ArmorHider;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

/**
 * Custom Gson serializer for ConfigurationSource classes.
 * Automatically initializes null ConfigurationItemBase fields after deserialization.
 * Additionally, sets a flag if a mismatch between declaration and serialized content is detected.
 */
public class ConfigurationSourceSerializer implements TypeAdapterFactory {

    public ConfigurationSourceSerializer() {
        // Initialize the factory registry on first instantiation
        ConfigurationItemFactoryRegistry.initialize();
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
                        Supplier<ConfigurationItemBase<?>> factory = ConfigurationItemFactoryRegistry.getDefaultFactory(field.getType());
                        if (factory != null) {
                            instance = factory.get();
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
