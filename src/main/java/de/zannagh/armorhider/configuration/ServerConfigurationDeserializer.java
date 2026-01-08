package de.zannagh.armorhider.configuration;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;

import java.io.IOException;

/**
 * Custom Gson TypeAdapter for ServerConfiguration that handles migration from older config formats.
 * <p>
 * This ensures that when ServerConfiguration is received over the network from servers
 * running older versions (e.g., 0.4.x with v3 format where enableCombatDetection was a top-level field),
 * the configuration is properly migrated to the current format.
 */
public class ServerConfigurationDeserializer implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        if (!ServerConfiguration.class.equals(typeToken.getRawType())) {
            return null;
        }

        TypeAdapter<T> defaultAdapter = gson.getDelegateAdapter(this, typeToken);

        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                defaultAdapter.write(out, value);
            }

            @Override
            public T read(JsonReader in) {
                JsonElement jsonElement = JsonParser.parseReader(in);

                if (!jsonElement.isJsonObject()) {
                    throw new JsonParseException("Expected JsonObject for ServerConfiguration");
                }

                JsonObject obj = jsonElement.getAsJsonObject();

                ServerConfiguration config = (ServerConfiguration) defaultAdapter.fromJsonTree(jsonElement);

                // Handle v3 format migration: enableCombatDetection was a top-level Boolean field
                if (obj.has("enableCombatDetection") && !obj.has("serverWideSettings")) {
                    Boolean legacyCombatDetection = obj.get("enableCombatDetection").getAsBoolean();
                    config.serverWideSettings = new ServerWideSettings(legacyCombatDetection);
                    ArmorHider.LOGGER.info("Migrated server config from v3 to v4 format via network (enableCombatDetection -> serverWideSettings).");
                    config.setHasChangedFromSerializedContent();
                } else if (config.serverWideSettings == null) {
                    // Fallback: if somehow serverWideSettings is still null, initialize with defaults
                    config.serverWideSettings = new ServerWideSettings();
                    ArmorHider.LOGGER.warn("ServerWideSettings was null after network deserialization, initialized with defaults.");
                }

                @SuppressWarnings("unchecked")
                T result = (T) config;
                return result;
            }
        };
    }
}
