package de.zannagh.armorhider.resources;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ServerConfiguration {
    Map<UUID, PlayerConfig> playerConfigs = new HashMap<>();
    
    Map<String, PlayerConfig> playerNameConfigs = new HashMap<>();
    
    public Boolean enableCombatDetection = true;

    public ServerConfiguration() {
    }

    public ServerConfiguration(Map<UUID, PlayerConfig> playerConfigs, Boolean enableCombatDetection) {
        this.playerConfigs = playerConfigs != null ? playerConfigs : new HashMap<>();
        this.enableCombatDetection = enableCombatDetection;
        this.playerConfigs.values().forEach(c -> playerNameConfigs.put(c.playerName, c));
    }

    public PlayerConfig getPlayerConfigOrDefault(PlayerEntity player) {
        if (getPlayerConfigOrDefault(player.getUuid()) instanceof PlayerConfig uuidConfig && Objects.equals(uuidConfig.playerName, Objects.requireNonNull(player.getDisplayName()).getString())) {
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

    public static ServerConfiguration fromLegacyFormat(Map<UUID, PlayerConfig> playerConfigs) {
        return new ServerConfiguration(playerConfigs, true);
    }
}
