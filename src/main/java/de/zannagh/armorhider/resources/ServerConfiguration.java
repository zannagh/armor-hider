package de.zannagh.armorhider.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.configuration.items.implementations.CombatDetection;
import de.zannagh.armorhider.netPackets.SettingsS2CPacket;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;

public class ServerConfiguration implements ConfigurationSource {
    private static final Type LEGACY_MAP_TYPE = new TypeToken<Map<UUID, PlayerConfig>>(){}.getType();
    private boolean hasChangedComparedToSerializedContent = false;
    Map<UUID, PlayerConfig> playerConfigs = new HashMap<>();
    
    Map<String, PlayerConfig> playerNameConfigs = new HashMap<>();

    @SerializedName(value = "enableCombatDetection")
    public CombatDetection enableCombatDetection;

    public ServerConfiguration() {
    }

    private ServerConfiguration(Map<UUID, PlayerConfig> playerConfigs, Boolean enableCombatDetection) {
        this.playerConfigs = playerConfigs != null ? playerConfigs : new HashMap<>();
        this.enableCombatDetection = new CombatDetection(enableCombatDetection);
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
    
    public void putOnRuntime(@NotNull String playerName, UUID playerId, PlayerConfig playerConfig) {
        playerNameConfigs.put(playerName, playerConfig);
        if (playerId != null) {
            playerConfigs.put(playerId, playerConfig);
        }
    }
    
    public static @NonNull ServerConfiguration deserialize(Reader reader) throws IOException {
        JsonElement element = JsonParser.parseReader(reader);
        ServerConfiguration configuration;
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            // Check if this past version 3 (where playerConfigs where added as a separate field).
            if (obj.has("playerConfigs")) {
                // New format - deserialize directly
                configuration = ArmorHider.GSON.fromJson(element, ServerConfiguration.class);
                ArmorHider.LOGGER.info("Loaded server config (new format).");
                if (configuration.playerConfigs.values().stream().anyMatch(PlayerConfig::hasChangedFromSerializedContent) || configuration.playerNameConfigs.values().stream().anyMatch(PlayerConfig::hasChangedFromSerializedContent)) {
                    configuration.setHasChangedFromSerializedContent();
                }
                return configuration;
            } else {
                // Old format - it's a flat map of UUID -> PlayerConfig
                Map<UUID, PlayerConfig> legacyData = ArmorHider.GSON.fromJson(element, LEGACY_MAP_TYPE);
                if (legacyData != null) {
                    configuration = ServerConfiguration.fromLegacyFormat(legacyData);
                    ArmorHider.LOGGER.info("Migrated server config (legacy format).");
                    configuration.setHasChangedFromSerializedContent();
                    return configuration;
                }
            }
        }
        throw new IOException("Failed to deserialize server configuration. Invalid format or content.");
    }
    
    public static @NonNull ServerConfiguration deserialize(String content) throws IOException {
        StringReader reader = new StringReader(content);
        return deserialize(reader);
    }
    
    @Contract("_ -> new")
    public static @NonNull ServerConfiguration fromPacket(@NonNull SettingsS2CPacket packet){
        var serverMap = new HashMap<UUID, PlayerConfig>();
        packet.config().forEach(c -> serverMap.put(c.playerId.getValue(), c));
        return new ServerConfiguration(serverMap, packet.serverCombatDetection());
    }

    @Contract("_ -> new")
    private static @NonNull ServerConfiguration fromLegacyFormat(Map<UUID, PlayerConfig> playerConfigs) {
        return new ServerConfiguration(playerConfigs, true);
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
