package de.zannagh.armorhider.paper.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.zannagh.armorhider.paper.net.PayloadCodec;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes {@code <level root>/armor-hider.json}, the same file and format the mod uses,
 * so a server can move between the mod and this plugin without losing player configs.
 *
 * <p>The level root is resolved by {@link LevelRootLocator}; read its javadoc before touching the
 * path, it is not {@code World#getWorldFolder()} for a reason.</p>
 */
public final class ServerConfigStorage {

    private final Path file;
    private final Path legacyFile;
    private final Logger logger;
    private final Object saveLock = new Object();

    /**
     * @param file       the level-scoped config, {@code <level root>/armor-hider.json}
     * @param legacyFile the pre-world-scoped config, {@code config/armor-hider-server.json}
     */
    public ServerConfigStorage(Path file, Path legacyFile, Logger logger) {
        this.file = file;
        this.legacyFile = legacyFile;
        this.logger = logger;
    }

    /**
     * Rescues a config left in the overworld <em>dimension</em> directory by a plugin build from
     * before the level-root fix.
     *
     * <p>Up to and including that build the path came from {@code World#getWorldFolder()}, which on
     * Paper 26.x is {@code <level root>/dimensions/minecraft/overworld} rather than the level root
     * (see {@link LevelRootLocator}). Servers that already ran such a build have real player state
     * there; moving it keeps it. The move is skipped when the canonical file already exists, so a
     * later boot never overwrites current state with the stale copy.</p>
     *
     * @param strandedFile the old {@code getWorldFolder()} config path, or {@code null} when the two
     *                     paths coincide (every 1.21.x server) and there is nothing to migrate
     */
    public void migrateDimensionFolderConfigIfNeeded(Path strandedFile) {
        if (strandedFile == null || !Files.exists(strandedFile) || Files.exists(file)) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.move(strandedFile, file, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Migrated config out of the dimension directory (" + strandedFile
                    + ") to the level root: " + file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to migrate config from " + strandedFile, e);
        }
    }

    /** Copies the old global config into the world folder if the world has none yet. */
    public void migrateGlobalConfigIfNeeded() {
        if (!Files.exists(legacyFile) || Files.exists(file)) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.copy(legacyFile, file);
            logger.info("Migrated global config to world: " + file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to migrate config", e);
        }
    }

    /** Loads the config, creating a default one if the file is missing or unreadable. */
    public ServerConfigurationState load() {
        ServerConfigurationState state = new ServerConfigurationState();
        if (!Files.exists(file)) {
            logger.info("Setup new server config due to missing file.");
            save(state);
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonObject()) {
                throw new IOException("Server config is not a JSON object");
            }
            readInto(state, element.getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            logger.log(Level.SEVERE, "Server config load failed - starting from defaults", e);
            return new ServerConfigurationState();
        }
        return state;
    }

    /** Writes the current state, pretty-printed, replacing the file atomically where possible. */
    public void save(ServerConfigurationState state) {
        JsonObject document = state.toJson();
        synchronized (saveLock) {
            try {
                Files.createDirectories(file.getParent());
                Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
                try (Writer writer = Files.newBufferedWriter(temporary)) {
                    PayloadCodec.gson().toJson(document, writer);
                }
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | RuntimeException e) {
                logger.log(Level.SEVERE, "Server config save failed", e);
            }
        }
    }

    private void readInto(ServerConfigurationState state, JsonObject root) {
        if (!root.has("playerConfigs")) {
            // v1/v2: the document itself is a flat UUID -> PlayerConfig map.
            readPlayerConfigs(state, root);
            logger.info("Migrated server config from legacy format (v1/v2).");
            return;
        }

        if (root.has("serverWideSettings") && root.get("serverWideSettings").isJsonObject()) {
            state.setServerWideSettings(root.getAsJsonObject("serverWideSettings"));
        } else if (root.has("enableCombatDetection")) {
            // v3: a bare enableCombatDetection boolean at the document root.
            JsonObject settings = ServerWideSettingsDefaults.create();
            settings.addProperty(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION,
                    root.get("enableCombatDetection").getAsBoolean());
            state.setServerWideSettings(settings);
            logger.info("Migrated server config from v3 to v4 format (enableCombatDetection -> serverWideSettings).");
        }

        if (root.get("playerConfigs").isJsonObject()) {
            readPlayerConfigs(state, root.getAsJsonObject("playerConfigs"));
        }
    }

    private void readPlayerConfigs(ServerConfigurationState state, JsonObject byId) {
        for (Map.Entry<String, JsonElement> entry : byId.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject config = entry.getValue().getAsJsonObject();
            UUID uuid = parseUuid(entry.getKey());
            if (uuid == null) {
                uuid = ServerConfigurationState.readPlayerId(config);
            }
            if (uuid == null) {
                continue;
            }
            state.getPlayerConfigs().put(uuid, config);
            String name = ServerConfigurationState.readPlayerName(config);
            if (name != null) {
                state.getPlayerNameConfigs().put(name, config);
            }
        }
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
