package de.zannagh.armorhider.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.ConfigurationSource;
//? if >= 1.20.5 {
import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

//? if >= 1.21.11 {
import net.minecraft.resources.Identifier;
//?}
//? if >= 1.20.5 && < 1.21.11 {
/*import net.minecraft.resources.ResourceLocation;
*///?}

public class ServerConfiguration implements ConfigurationSource<ServerConfiguration> {

    //? if >= 1.21.11 {
    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "settings_s2c_packet");
    //?}
    //? if >= 1.20.5 && < 1.21.11 {
    /*public static final ResourceLocation PACKET_IDENTIFIER = ResourceLocation.fromNamespaceAndPath("armorhider", "settings_s2c_packet");
    *///?}

    //? if >= 1.20.5 {
    public static final Type<ServerConfiguration> TYPE = new Type<>(PACKET_IDENTIFIER);
    
    public static final StreamCodec<ByteBuf, ServerConfiguration> STREAM_CODEC = CompressedJsonCodec.create(ServerConfiguration.class);
    
    public StreamCodec<ByteBuf, ServerConfiguration> getCodec() {
        return CompressedJsonCodec.create(ServerConfiguration.class);
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    private static final java.lang.reflect.Type LEGACY_MAP_TYPE = new TypeToken<Map<UUID, PlayerConfig>>() {
    }.getType();
    @SerializedName(value = "serverWideSettings")
    public ServerWideSettings serverWideSettings;
    Map<UUID, PlayerConfig> playerConfigs = new HashMap<>();

    Map<String, PlayerConfig> playerNameConfigs = new HashMap<>();
    private transient boolean hasChangedComparedToSerializedContent = false;

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
                    configuration.serverWideSettings = new ServerWideSettings(legacyCombatDetection, false);
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
        return new ServerConfiguration(playerConfigs, new ServerWideSettings(true, false));
    }


    public PlayerConfig getPlayerConfigOrDefault(Player player) {
        PlayerConfig uuidConfig = getPlayerConfigOrDefault(player.getUUID());
        if (uuidConfig != null && Objects.equals(uuidConfig.playerName.getValue(), Objects.requireNonNull(player.getDisplayName()).getString())) {
            return uuidConfig;
        }
        return getPlayerConfigOrDefault(Objects.requireNonNull(player.getDisplayName()).getString());
    }

    public PlayerConfig getPlayerConfigOrDefault(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return playerConfigs.getOrDefault(uuid, null);
    }

    public PlayerConfig getPlayerConfigOrDefault(String name) {
        if (name == null) {
            return null;
        }
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

    public void put(PlayerConfig playerConfig) {
        playerNameConfigs.put(playerConfig.playerName.getValue(), playerConfig);
        playerConfigs.put(playerConfig.playerId.getValue(), playerConfig);
    }

    public String toJson() {
        return ArmorHider.GSON.toJson(this);
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
