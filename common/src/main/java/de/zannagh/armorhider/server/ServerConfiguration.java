package de.zannagh.armorhider.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.util.PlayerNameUtil;

import de.zannagh.armorhider.net.CompressedJsonCodec;
import de.zannagh.armorhider.net.packets.PlayerConfig;
//? if >= 1.20.5 {
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
import java.util.function.Consumer;

//? if >= 1.21.11 {
/*import net.minecraft.resources.ResourceLocation;
*///?}
//? if >= 1.20.5 && < 1.21.11 {
import net.minecraft.resources.ResourceLocation;
//?}

public class ServerConfiguration implements ConfigurationSource<ServerConfiguration> {

    //? if >= 1.21.11 {
    /*public static final ResourceLocation PACKET_IDENTIFIER = ResourceLocation.fromNamespaceAndPath("de.zannagh.armorhider", "settings_s2c_packet");
    *///?}
    //? if >= 1.20.5 && < 1.21.11 {
    public static final ResourceLocation PACKET_IDENTIFIER = ResourceLocation.fromNamespaceAndPath("armorhider", "settings_s2c_packet");
    //?}

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
    public @NonNull ServerWideSettings serverWideSettings;
    Map<UUID, PlayerConfig> playerConfigs = new HashMap<>();

    Map<String, PlayerConfig> playerNameConfigs = new HashMap<>();
    private transient boolean hasChangedComparedToSerializedContent = false;

    public ServerConfiguration() {
        // Always initialize serverWideSettings with defaults to prevent null pointer exceptions
        // Migration logic will detect v3 format by checking for old field presence
        this.serverWideSettings = ServerWideSettings.defaults();
    }

    private ServerConfiguration(Map<UUID, PlayerConfig> playerConfigs, @NonNull ServerWideSettings serverWideSettings) {
        this.playerConfigs = playerConfigs != null ? playerConfigs : new HashMap<>();
        this.serverWideSettings = serverWideSettings;
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
                    configuration.serverWideSettings = new ServerWideSettings(legacyCombatDetection, false, false, true);
                    ArmorHider.LOGGER.info("Migrated server config from v3 to v4 format (enableCombatDetection -> serverWideSettings).");
                    configuration.setHasChangedFromSerializedContent();
                } else {
                    ArmorHider.LOGGER.info("Loaded server config (v4 format).");
                }

                // Route the linear schema migration of every embedded config item (server-wide settings +
                // per-player configs) through the shared mechanism. ensureSchemaFrom(...) flags this
                // container as changed whenever any embedded item was migrated.
                configuration = configuration.ensureSchemaFrom(configuration);

                // Also propagate a changed flag that an embedded item may have set for a non-schema reason
                // (e.g. a missing field default-initialized during deserialization) so it gets re-saved.
                if (configuration.serverWideSettings.hasChangedFromSerializedContent()
                        || configuration.playerConfigs.values().stream()
                                .anyMatch(PlayerConfig::hasChangedFromSerializedContent)) {
                    configuration.setHasChangedFromSerializedContent();
                }

                // Rebuild playerNameConfigs from the (possibly migrated) playerConfigs
                configuration.rebuildPlayerNameConfigs();

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
        return new ServerConfiguration(playerConfigs, new ServerWideSettings(true, false, false, true));
    }


    public PlayerConfig getPlayerConfigOrDefault(Player player) {
        PlayerConfig uuidConfig = getPlayerConfigOrDefault(player.getUUID());
        String name = PlayerNameUtil.getPlayerName(player);
        if (uuidConfig != null && Objects.equals(uuidConfig.playerName.getValue(), name)) {
            return uuidConfig;
        }
        return getPlayerConfigOrDefault(name);
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

    private final List<Consumer<String>> configListeners = new ArrayList<>();

    public void addConfigChangeListener(Consumer<String> listener) {
        configListeners.add(listener);
    }

    private void notifyConfigListeners(String playerName) {
        configListeners.forEach(l -> l.accept(playerName));
    }

    public void put(@NotNull String playerName, UUID playerId, PlayerConfig playerConfig) {
        playerNameConfigs.put(playerName, playerConfig);
        if (playerId != null) {
            playerConfigs.put(playerId, playerConfig);
        }
        notifyConfigListeners(playerName);
    }

    public void put(PlayerConfig playerConfig) {
        playerNameConfigs.put(playerConfig.playerName.getValue(), playerConfig);
        playerConfigs.put(playerConfig.playerId.getValue(), playerConfig);
        notifyConfigListeners(playerConfig.playerName.getValue());
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

    // ServerConfiguration is a container. Its structural (shape) migration — legacy UUID map -> v3 -> v4 —
    // is format-aware and stays in deserialize(...)/ServerConfigurationDeserializer, because those input
    // shapes aren't ServerConfiguration objects yet and so can't be expressed as migrateFrom(old). Everything
    // downstream of that is per-item linear schema migration (the embedded ServerWideSettings' schema and
    // every embedded PlayerConfig's schema) and is routed through the shared ConfigurationSource mechanism
    // via shouldMigrate()/migrateFrom()/ensureSchemaFrom().

    @Override
    public int getSchemaVersion() {
        return serverWideSettings.getSchemaVersion();
    }

    @Override
    public int getCurrentSchemaVersion() {
        return serverWideSettings.getCurrentSchemaVersion();
    }

    /**
     * A container is out of date whenever any embedded config item is: the server-wide settings or any
     * per-player config. The default ({@code schema < currentSchema}) only reflects the server-wide
     * settings, so it is widened here to also account for the embedded player configs.
     */
    @Override
    public boolean shouldMigrate() {
        return serverWideSettings.shouldMigrate()
                || playerConfigs.values().stream().anyMatch(PlayerConfig::shouldMigrate);
    }

    /**
     * Migrates every embedded config item to the current schema through its own
     * {@link ConfigurationSource#ensureSchemaFrom} and rebuilds the by-name index from the migrated player
     * configs. The container is mutated in place and returned.
     */
    @Override
    public ServerConfiguration migrateFrom(ServerConfiguration old) {
        old.serverWideSettings = old.serverWideSettings.ensureSchemaFrom(old.serverWideSettings);
        old.playerConfigs.replaceAll((uuid, pc) -> pc.ensureSchemaFrom(pc));
        old.rebuildPlayerNameConfigs();
        return old;
    }

    /** Rebuilds {@link #playerNameConfigs} from the current {@link #playerConfigs} values. */
    private void rebuildPlayerNameConfigs() {
        playerNameConfigs.clear();
        playerConfigs.values().forEach(c -> playerNameConfigs.put(c.playerName.getValue(), c));
    }
}
