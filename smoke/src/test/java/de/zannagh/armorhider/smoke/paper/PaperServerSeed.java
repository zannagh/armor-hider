package de.zannagh.armorhider.smoke.paper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Writes and reads back {@code <level root>/armor-hider.json} - the one canonical path the plugin
 * resolves via {@code new File(Bukkit.getWorldContainer(), overworld.getName())}.
 *
 * <p>The seeded values are deliberately the inverse of every shipped default, so a client-side
 * assertion that observes them has proved the document travelled over the wire rather than being
 * reconstructed locally. Config items serialize as <em>bare values</em>, not {@code {"value": ...}}
 * wrappers.</p>
 *
 * <p>This harness used to seed the overworld dimension directory as well, because the plugin
 * resolved its path from {@code World#getWorldFolder()}, which is the dimension directory on Paper
 * 26.x. That is now a bug the assertions catch rather than a layout the harness tolerates:
 * {@link #assertSeedSurvived(Path)} fails if a config shows up anywhere but the level root.</p>
 */
public final class PaperServerSeed {

    /** File name the plugin looks for inside the level root. */
    public static final String CONFIG_FILE_NAME = "armor-hider.json";

    /** Seeded value - inverse of the default {@code true}. */
    public static final boolean SEEDED_ENABLE_COMBAT_DETECTION = false;

    /** Seeded value - inverse of the default {@code false}. */
    public static final boolean SEEDED_FORCE_ARMOR_HIDER_OFF = true;

    /** Seeded value - inverse of the default {@code false}. */
    public static final boolean SEEDED_DISABLE_ON_INVISIBILITY = true;

    /** Seeded value - inverse of the default {@code true}. */
    public static final boolean SEEDED_ALLOW_INDIVIDUAL_CONFIGURATIONS = false;

    /** {@code ServerWideSettingsDefaults.CURRENT_CONFIG_VERSION}. */
    private static final int CONFIG_VERSION = 2;

    private PaperServerSeed() {
    }

    /**
     * Writes the pre-seeded config to {@code <level root>/armor-hider.json}, creating the level root
     * if the server has not generated the world yet.
     *
     * @param levelDirectory {@code <world-dir>/<level-name>}, i.e. {@code .../world} by default
     * @return the path written
     */
    public static Path write(Path levelDirectory) throws IOException {
        Files.createDirectories(levelDirectory);
        Path file = canonicalConfigFile(levelDirectory);
        Files.writeString(file, document(), StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Writes the pre-seeded config into the pre-fix location - the overworld dimension directory -
     * to exercise the plugin's migration back to the level root.
     *
     * <p>The directory must already exist: pre-creating it makes Paper 26.2 abort with "Unable to
     * read or access the world gen settings file for dimension minecraft:overworld", so the caller
     * has to generate the world first.</p>
     */
    public static Path writeStranded(Path levelDirectory) throws IOException {
        Path directory = dimensionDirectory(levelDirectory);
        if (!Files.isDirectory(directory)) {
            throw new IOException("Dimension directory does not exist yet: " + directory);
        }
        Path file = directory.resolve(CONFIG_FILE_NAME);
        Files.writeString(file, document(), StandardCharsets.UTF_8);
        return file;
    }

    /** The only path the plugin may use: {@code <level root>/armor-hider.json}. */
    public static Path canonicalConfigFile(Path levelDirectory) {
        return levelDirectory.resolve(CONFIG_FILE_NAME);
    }

    /** {@code <level root>/dimensions/minecraft/overworld} - the 26.x {@code getWorldFolder()}. */
    public static Path dimensionDirectory(Path levelDirectory) {
        return levelDirectory.resolve("dimensions").resolve("minecraft").resolve("overworld");
    }

    /** All {@code armor-hider.json} files that exist beneath a level directory. */
    public static List<Path> existingConfigFiles(Path levelDirectory) throws IOException {
        if (!Files.isDirectory(levelDirectory)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(levelDirectory)) {
            return walk.filter(path -> path.getFileName().toString().equals(CONFIG_FILE_NAME))
                    .sorted()
                    .toList();
        }
    }

    /**
     * The exact JSON the plugin's {@code ServerConfigStorage.load()} expects: a
     * {@code serverWideSettings} block of bare booleans plus the two (empty) config indexes.
     */
    public static String document() {
        return """
                {
                  "serverWideSettings": {
                    "configVersion": %d,
                    "enableCombatDetection": %b,
                    "forceArmorHiderOff": %b,
                    "disableArmorHiderOnInvisibilityGlobally": %b,
                    "allowIndividualPlayerConfigurations": %b
                  },
                  "playerConfigs": {},
                  "playerNameConfigs": {}
                }
                """.formatted(CONFIG_VERSION,
                SEEDED_ENABLE_COMBAT_DETECTION,
                SEEDED_FORCE_ARMOR_HIDER_OFF,
                SEEDED_DISABLE_ON_INVISIBILITY,
                SEEDED_ALLOW_INDIVIDUAL_CONFIGURATIONS);
    }

    /** Writes {@code ops.json}, granting the smoke player operator level 4 before first boot. */
    public static Path writeOps(Path serverDirectory) throws IOException {
        Files.createDirectories(serverDirectory);
        Path file = serverDirectory.resolve("ops.json");
        Files.writeString(file, SmokePlayer.opsJson(), StandardCharsets.UTF_8);
        return file;
    }

    /** Parses one on-disk config document. */
    public static Map<String, Object> read(Path configFile) throws IOException {
        if (!Files.isRegularFile(configFile)) {
            throw new IOException("No " + CONFIG_FILE_NAME + " at " + configFile);
        }
        return Json.object(Json.parse(Files.readString(configFile, StandardCharsets.UTF_8)));
    }

    /**
     * Asserts the config lives at exactly one path - the level root - and that the four seeded
     * booleans survived a plugin load/save round trip. If the plugin had not read the seed it would
     * write defaults back, so a surviving value proves the seeded file was the one loaded.
     *
     * @throws AssertionError if a config exists anywhere else (the 26.x dimension directory being
     *                        the regression this pins), or if any value drifted
     */
    public static void assertSeedSurvived(Path levelDirectory) throws IOException {
        Path file = assertConfigOnlyAtLevelRoot(levelDirectory);
        Map<String, Object> settings = Json.object(Json.path(read(file), "serverWideSettings"));
        expect(file, settings, "enableCombatDetection", SEEDED_ENABLE_COMBAT_DETECTION);
        expect(file, settings, "forceArmorHiderOff", SEEDED_FORCE_ARMOR_HIDER_OFF);
        expect(file, settings, "disableArmorHiderOnInvisibilityGlobally",
                SEEDED_DISABLE_ON_INVISIBILITY);
        expect(file, settings, "allowIndividualPlayerConfigurations",
                SEEDED_ALLOW_INDIVIDUAL_CONFIGURATIONS);
    }

    /**
     * Asserts that {@code armor-hider.json} exists at the level root and nowhere else below it.
     *
     * @return the canonical config file
     * @throws AssertionError if it is missing, or if a stray copy exists
     */
    public static Path assertConfigOnlyAtLevelRoot(Path levelDirectory) throws IOException {
        Path canonical = canonicalConfigFile(levelDirectory);
        List<Path> found = existingConfigFiles(levelDirectory);
        if (!found.contains(canonical)) {
            throw new AssertionError("No " + CONFIG_FILE_NAME + " at the level root " + canonical
                    + " - found instead: " + found);
        }
        List<Path> strays = new ArrayList<>(found);
        strays.remove(canonical);
        if (!strays.isEmpty()) {
            throw new AssertionError(CONFIG_FILE_NAME + " also exists outside the level root: "
                    + strays + " (getWorldFolder() regression?)");
        }
        return canonical;
    }

    /**
     * Asserts the saved config gained an entry for the smoke player under <em>both</em>
     * {@code playerConfigs} (keyed by UUID) and {@code playerNameConfigs} (keyed by display name)
     * - the proof that a client-to-server {@code PlayerConfig} packet arrived, was stored and was
     * persisted.
     *
     * <p>The by-name map matters independently: nothing rebuilds it client-side, so a server that
     * fails to emit it makes every client-side name lookup silently return null.</p>
     *
     * @throws AssertionError if the config does not carry both entries
     */
    public static void assertPlayerConfigStored(Path levelDirectory) throws IOException {
        assertPlayerConfigStored(levelDirectory, SmokePlayer.uuidString(), SmokePlayer.NAME);
    }

    /** Variant of {@link #assertPlayerConfigStored(Path)} for an arbitrary identity. */
    public static void assertPlayerConfigStored(Path levelDirectory, String playerId,
                                                String playerName) throws IOException {
        Path file = assertConfigOnlyAtLevelRoot(levelDirectory);
        Map<String, Object> root = read(file);
        Map<String, Object> byId = Json.object(Json.path(root, "playerConfigs"));
        Map<String, Object> byName = Json.object(Json.path(root, "playerNameConfigs"));
        if (byId.containsKey(playerId) && byName.containsKey(playerName)) {
            return;
        }
        throw new AssertionError("Config did not store " + playerId + " / " + playerName + ":\n"
                + file + " -> playerConfigs=" + byId.keySet()
                + ", playerNameConfigs=" + byName.keySet());
    }

    private static void expect(Path file, Map<String, Object> settings, String key,
                               boolean expected) {
        Object actual = settings.get(key);
        if (!Boolean.valueOf(expected).equals(actual)) {
            throw new AssertionError("Seeded " + key + " expected " + expected + " but was " + actual
                    + " in " + file);
        }
    }
}
