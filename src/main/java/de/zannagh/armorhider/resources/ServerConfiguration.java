package de.zannagh.armorhider.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

public class ServerConfiguration implements ConfigurationSource<ServerConfiguration> {
    
    public static final Id<ServerConfiguration> PACKET_IDENTIFIER = new Id<>(Identifier.of("de.zannagh.armorhider", "settings_s2c_packet"));

    public PacketCodec<ByteBuf, ServerConfiguration> getCodec() {
        return CompressedJsonCodec.create(ServerConfiguration.class);
    }

    @Override
    public Id<ServerConfiguration> getId() {
        return PACKET_IDENTIFIER;
    }
    
    private static final java.lang.reflect.Type LEGACY_MAP_TYPE = new TypeToken<Map<UUID, PlayerConfig>>(){}.getType();
    private boolean hasChangedComparedToSerializedContent = false;
    Map<UUID, PlayerConfig> playerConfigs = new HashMap<>();
    
    Map<String, PlayerConfig> playerNameConfigs = new HashMap<>();

    @SerializedName(value = "serverWideSettings")
    public ServerWideSettings serverWideSettings;

    public ServerConfiguration() {
        // Always initialize serverWideSettings with defaults to prevent null pointer exceptions
        // Migration logic will detect v3 format by checking for old field presence
        this.serverWideSettings = new ServerWideSettings();
    }

    private ServerConfiguration(Map<UUID, PlayerConfig> playerConfigs, ServerWideSettings serverWideSettings) {
        this.playerConfigs = playerConfigs != null ? playerConfigs : new HashMap<>();
        this.serverWideSettings = serverWideSettings != null ? serverWideSettings : new ServerWideSettings();
        this.playerConfigs.values().forEach(c -> playerNameConfigs.put(c.playerName.getValue(), c));
    }

    public PlayerConfig getPlayerConfigOrDefault(PlayerEntity player) {
        if (getPlayerConfigOrDefault(player.getUuid()) instanceof PlayerConfig uuidConfig && Objects.equals(uuidConfig.playerName.getValue(), Objects.requireNonNull(player.getDisplayName()).getString())) {
            return uuidConfig;
        }
        return getPlayerConfigOrDefault(Objects.requireNonNull(player.getDisplayName()).getString());
    }
    
    public PlayerConfig getPlayerConfigOrDefault(UUID uuid) {
        return playerConfigs.getOrDefault(uuid, null);
    }
    
    public PlayerConfig getPlayerConfigOrDefault(String name) {
        return playerNameConfigs.getOrDefault(name, null);
    }
    
    public List<PlayerConfig> getPlayerConfigs() {
        return new ArrayList<>(playerConfigs.values());
    }
    
    public void put(@NotNull String playerName, UUID playerId, PlayerConfig playerConfig) {
        playerNameConfigs.put(playerName, playerConfig);
        if (playerId != null) {
            playerConfigs.put(playerId, playerConfig);
        }
    }
    
    public String toJson() {
        return ArmorHider.GSON.toJson(this);
    }
    
    public static @NotNull ServerConfiguration deserialize(Reader reader) throws IOException {
        JsonElement element = JsonParser.parseReader(reader);
        ServerConfiguration configuration;
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            // Check if this is version 3+ (where playerConfigs was added as a separate field).
            if (obj.has("playerConfigs")) {
                // Deserialize the base structure
                configuration = ArmorHider.GSON.fromJson(element, ServerConfiguration.class);

                // Check if we need to migrate from v3 format (enableCombatDetection boolean) to v4 (serverWideSettings object)
                // Detect v3 by presence of old field and absence of new field in JSON
                if (obj.has("enableCombatDetection") && !obj.has("serverWideSettings")) {
                    Boolean legacyCombatDetection = obj.get("enableCombatDetection").getAsBoolean();
                    configuration.serverWideSettings = new ServerWideSettings(legacyCombatDetection);
                    ArmorHider.LOGGER.info("Migrated server config from v3 to v4 format (enableCombatDetection -> serverWideSettings).");
                    configuration.setHasChangedFromSerializedContent();
                } else if (configuration.serverWideSettings == null) {
                    // Safety fallback: if somehow serverWideSettings is still null, initialize with defaults
                    configuration.serverWideSettings = new ServerWideSettings();
                    ArmorHider.LOGGER.warn("ServerWideSettings was null after deserialization, initialized with defaults.");
                } else {
                    ArmorHider.LOGGER.info("Loaded server config (v4 format).");
                }

                // Check if any player configs changed during deserialization
                if (configuration.playerConfigs.values().stream().anyMatch(PlayerConfig::hasChangedFromSerializedContent) ||
                    configuration.playerNameConfigs.values().stream().anyMatch(PlayerConfig::hasChangedFromSerializedContent) ||
                    configuration.serverWideSettings.hasChangedFromSerializedContent()) {
                    configuration.setHasChangedFromSerializedContent();
                }

                // Rebuild playerNameConfigs map if needed
                configuration.playerConfigs.values().forEach(c ->
                    configuration.playerNameConfigs.put(c.playerName.getValue(), c));

                return configuration;
            } else {
                // Old format (v1/v2) - it's a flat map of UUID -> PlayerConfig
                Map<UUID, PlayerConfig> legacyData = ArmorHider.GSON.fromJson(element, LEGACY_MAP_TYPE);
                if (legacyData != null) {
                    configuration = ServerConfiguration.fromLegacyFormat(legacyData);
                    ArmorHider.LOGGER.info("Migrated server config from legacy format (v1/v2).");
                    configuration.setHasChangedFromSerializedContent();
                    return configuration;
                }
            }
        }
        throw new IOException("Failed to deserialize server configuration. Invalid format or content.");
    }
    
    public static @NotNull ServerConfiguration deserialize(String content) throws IOException {
        StringReader reader = new StringReader(content);
        return deserialize(reader);
    }

    @Contract("_ -> new")
    private static @NotNull ServerConfiguration fromLegacyFormat(Map<UUID, PlayerConfig> playerConfigs) {
        return new ServerConfiguration(playerConfigs, new ServerWideSettings(true));
    }

    @Override
    public boolean hasChangedFromSerializedContent() {
        return hasChangedComparedToSerializedContent;
    }

    @Override
    public void setHasChangedFromSerializedContent() {
        hasChangedComparedToSerializedContent = true;
    }
}
