package de.zannagh.armorhider.paper.config;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolves the level root - the directory the mod calls {@code LevelResource.ROOT} - so the plugin
 * and the mod share one {@code armor-hider.json}.
 *
 * <p><strong>Do not "simplify" this to {@code World#getWorldFolder()}.</strong> That method does not
 * mean the same thing on every Paper line, which is exactly the bug this class exists to fix:</p>
 *
 * <table>
 *   <caption>Verified on real servers, 2026-07-28</caption>
 *   <tr><th>Paper</th><th>{@code getWorldFolder()} returns</th></tr>
 *   <tr><td>1.21.8</td><td>the level root - correct, but only by accident</td></tr>
 *   <tr><td>26.2</td><td>{@code worlds/world/dimensions/minecraft/overworld} - the
 *       <em>dimension</em> directory</td></tr>
 * </table>
 *
 * <p>On 26.x the plugin therefore wrote its config into the dimension directory and ignored a
 * config seeded at the level root, so a server migrating from the Fabric/NeoForge mod silently lost
 * its settings. {@code new File(Bukkit.getWorldContainer(), overworld.getName())} is stable across
 * both layouts: on 26.2 the container is {@code worlds/} and the overworld is named {@code world}
 * ({@code worlds/world}); on older Paper the container is the server root ({@code ./world}). Both
 * are what {@code LevelResource.ROOT} points at.</p>
 *
 * @see ServerConfigStorage#migrateDimensionFolderConfigIfNeeded(Path)
 */
public final class LevelRootLocator {

    /** File name shared with the mod. */
    public static final String CONFIG_FILE_NAME = "armor-hider.json";

    private LevelRootLocator() {
    }

    /**
     * The level root, or {@code fallback} when no world is loaded yet.
     *
     * <p>{@code Bukkit.getWorlds()} is empty during {@code onLoad} and on a server whose worlds all
     * failed to load, so the empty case is handled rather than allowed to throw out of
     * {@code onEnable}.</p>
     *
     * @param fallback directory to use when the overworld cannot be determined, typically the
     *                 plugin's data folder
     */
    public static Path levelRoot(File fallback) {
        World overworld = overworld();
        if (overworld == null) {
            return fallback.toPath();
        }
        return new File(Bukkit.getWorldContainer(), overworld.getName()).toPath();
    }

    /** {@code <level root>/armor-hider.json} - the canonical config location. */
    public static Path configFile(File fallback) {
        return levelRoot(fallback).resolve(CONFIG_FILE_NAME);
    }

    /**
     * The path older plugin builds used, i.e. {@code World#getWorldFolder()}, or {@code null} when
     * no world is loaded or that path is already the level root.
     *
     * <p>Only 26.x ever differs; on 1.21.x this returns {@code null} because there is nothing to
     * migrate. Kept as an explicit method so {@link ServerConfigStorage} can rescue state left
     * behind by a plugin build that shipped before the fix.</p>
     */
    public static Path strandedDimensionConfigFile(File fallback) {
        World overworld = overworld();
        if (overworld == null) {
            return null;
        }
        Path stranded = overworld.getWorldFolder().toPath().toAbsolutePath().normalize();
        Path canonical = levelRoot(fallback).toAbsolutePath().normalize();
        if (stranded.equals(canonical)) {
            return null;
        }
        return stranded.resolve(CONFIG_FILE_NAME);
    }

    /** The overworld, or {@code null} if no world is loaded. */
    private static World overworld() {
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
