package de.zannagh.armorhider.paper.config;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server's entire understanding of the protocol.
 *
 * <p>Only {@code playerId} and {@code playerName} are ever read out of a player config; the other
 * ~28 fields are stored and relayed opaquely as raw {@link JsonObject}s, which is what makes this
 * plugin schema-agnostic - a client shipping a newer {@code PlayerConfig} schema round-trips
 * unchanged.</p>
 */
public final class ServerConfigurationState {

    public static final String PLAYER_ID = "playerId";

    public static final String PLAYER_NAME = "playerName";

    /**
     * Makes a mutation and a snapshot mutually exclusive.
     *
     * <p>The two maps are individually concurrent but are updated in <em>separate steps</em> by
     * {@link #put}, so an unguarded {@link #toJson} racing a {@code put} can observe a config that
     * is already in {@code playerConfigs} but not yet in {@code playerNameConfigs}. That torn
     * document is not self-healing: nothing rebuilds {@code playerNameConfigs} client-side (see
     * {@link #toJson}), so persisting or broadcasting it makes the client's primary by-name lookup
     * silently return null for that player.</p>
     *
     * <p>Two callers make this reachable rather than theoretical: saves run off-thread via
     * {@code Schedulers#runAsync}, and on Folia the inbound handlers themselves run on regionised
     * threads, so two players in different regions can be inside {@code put} at the same time.</p>
     *
     * <p>Only the in-memory tree build is guarded; the disk write in {@code ServerConfigStorage}
     * stays outside, so the lock is never held across I/O.</p>
     */
    private final Object snapshotLock = new Object();

    private final Map<UUID, JsonObject> playerConfigs = new ConcurrentHashMap<>();
    private final Map<String, JsonObject> playerNameConfigs = new ConcurrentHashMap<>();
    private volatile JsonObject serverWideSettings = ServerWideSettingsDefaults.create();

    /** Returns the live server-wide settings object. Callers must not mutate it in place. */
    public JsonObject getServerWideSettings() {
        return serverWideSettings;
    }

    public void setServerWideSettings(JsonObject settings) {
        synchronized (snapshotLock) {
            serverWideSettings = ServerWideSettingsDefaults.fillMissing(settings);
        }
    }

    public Map<UUID, JsonObject> getPlayerConfigs() {
        return playerConfigs;
    }

    public Map<String, JsonObject> getPlayerNameConfigs() {
        return playerNameConfigs;
    }

    /**
     * Stores a player config, reproducing the name-collision reconciliation of the mod's
     * {@code ServerConfigStore.put}: every existing entry whose {@code playerName} matches the
     * incoming config's is re-asserted, and the by-name index is repointed at the new config.
     */
    public void put(UUID uuid, JsonObject config) {
        synchronized (snapshotLock) {
            playerConfigs.put(uuid, config);

            String name = readPlayerName(config);
            if (name == null) {
                return;
            }

            Map<UUID, JsonObject> overwrites = new HashMap<>();
            playerConfigs.forEach((existingId, existing) -> {
                if (name.equals(readPlayerName(existing))) {
                    overwrites.put(existingId, existing);
                }
            });
            playerNameConfigs.put(name, config);
            overwrites.forEach(playerConfigs::replace);
        }
    }

    /**
     * Builds the clientbound/on-disk {@code ServerConfiguration} document.
     *
     * <p>{@code playerNameConfigs} is <em>not</em> transient in the mod and nothing rebuilds it
     * client-side, so it must be emitted: the client's primary lookup is by name, and omitting the
     * map makes every lookup silently return null.</p>
     */
    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        synchronized (snapshotLock) {
            root.add("serverWideSettings", serverWideSettings.deepCopy());

            JsonObject byId = new JsonObject();
            playerConfigs.forEach((uuid, config) -> byId.add(uuid.toString(), config));
            root.add("playerConfigs", byId);

            JsonObject byName = new JsonObject();
            playerNameConfigs.forEach(byName::add);
            root.add("playerNameConfigs", byName);
        }
        return root;
    }

    /** Reads the {@code playerName} bare string value, or {@code null} if absent/malformed. */
    public static String readPlayerName(JsonObject config) {
        if (config == null || !config.has(PLAYER_NAME) || !config.get(PLAYER_NAME).isJsonPrimitive()) {
            return null;
        }
        return config.get(PLAYER_NAME).getAsString();
    }

    /** Reads the {@code playerId} bare string value as a UUID, or {@code null} if absent/malformed. */
    public static UUID readPlayerId(JsonObject config) {
        if (config == null || !config.has(PLAYER_ID) || !config.get(PLAYER_ID).isJsonPrimitive()) {
            return null;
        }
        try {
            return UUID.fromString(config.get(PLAYER_ID).getAsString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
