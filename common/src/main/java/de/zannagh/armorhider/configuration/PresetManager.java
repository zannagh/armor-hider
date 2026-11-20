package de.zannagh.armorhider.configuration;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class PresetManager {

    public static final int PRESET_COUNT = 5;
    public static final int DEFAULT_QUICK_TOGGLE_A = 0;
    public static final int DEFAULT_QUICK_TOGGLE_B = 1;
    private static final Path DEFAULT_FILE = new File("config", "armor-hider-presets.json").toPath();

    private final Path file;
    private final ConfigPreset @Nullable [] presets = new ConfigPreset[PRESET_COUNT];
    private int activeIndex = -1;
    private int quickToggleA = DEFAULT_QUICK_TOGGLE_A;
    private int quickToggleB = DEFAULT_QUICK_TOGGLE_B;
    private boolean quickToggleKeyChecked;
    private boolean quickToggleNoticePending;
    /** Set by {@link #updateActivePreset}; cleared by {@link #save()}. See {@link #flushPendingSave()}. */
    private boolean pendingSave;

    public PresetManager() {
        this(DEFAULT_FILE);
    }

    public PresetManager(Path file) {
        this.file = file;
        load();
    }

    public @Nullable ConfigPreset getPreset(int index) {
        if (index < 0 || index >= PRESET_COUNT) return null;
        return presets[index];
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int index) {
        this.activeIndex = index;
        save();
    }

    public void deactivate() {
        this.activeIndex = -1;
        save();
    }

    public boolean isActive(int index) {
        return activeIndex == index;
    }

    public void savePreset(int index, ConfigPreset preset) {
        if (index < 0 || index >= PRESET_COUNT) return;
        presets[index] = preset.deepCopy();
        save();
    }

    public void saveFromCurrentConfig(int index, PlayerConfig config) {
        savePreset(index, ConfigPreset.fromPlayerConfig(config));
    }

    /**
     * Syncs the live config into the active preset <em>in memory only</em>, marking it for a later write.
     * <p>
     * This is called from every settings mutation, including slider movement. Writing here is what made a
     * single slider drag issue a synchronous, pretty-printed serialization of all five presets per frame on
     * the render thread. Callers flush via {@link #flushPendingSave()} when the settings screen closes.
     */
    public void updateActivePreset(PlayerConfig config) {
        // Deliberately does NOT require presets[activeIndex] to be non-null: the preset is rebuilt wholesale
        // from the live config, so a missing entry is something to materialise, not a reason to skip. Bailing
        // out there would silently drop every settings change for as long as that slot stayed active.
        if (activeIndex >= 0 && activeIndex < PRESET_COUNT) {
            presets[activeIndex] = ConfigPreset.fromPlayerConfig(config);
            pendingSave = true;
        }
    }

    /** Writes presets to disk if {@link #updateActivePreset} left changes pending. */
    public void flushPendingSave() {
        if (pendingSave) {
            save();
        }
    }

    public int getQuickToggleA() {
        return quickToggleA;
    }

    public int getQuickToggleB() {
        return quickToggleB;
    }

    /** Sets the two presets the quick-toggle keybind alternates between. Out-of-range indices are ignored. */
    public void setQuickTogglePresets(int a, int b) {
        if (!isValidIndex(a) || !isValidIndex(b)) {
            return;
        }
        quickToggleA = a;
        quickToggleB = b;
        save();
    }

    /**
     * The preset the quick-toggle should activate next: B while A is active, otherwise A (B active,
     * another preset, or none).
     */
    public int resolveQuickToggleTarget() {
        return activeIndex == quickToggleA ? quickToggleB : quickToggleA;
    }

    /** Whether the quick-toggle's default key was already checked for a conflict (done once per install). */
    public boolean isQuickToggleKeyChecked() {
        return quickToggleKeyChecked;
    }

    /** Whether the "key already taken" toast is still owed to the player. Survives restarts. */
    public boolean isQuickToggleNoticePending() {
        return quickToggleNoticePending;
    }

    public void setQuickToggleKeyState(boolean checked, boolean noticePending) {
        quickToggleKeyChecked = checked;
        quickToggleNoticePending = noticePending;
        save();
    }

    private static boolean isValidIndex(int index) {
        return index >= 0 && index < PRESET_COUNT;
    }

    public boolean hasPreset(int index) {
        return index >= 0 && index < PRESET_COUNT && presets[index] != null;
    }

    private void load() {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    var loaded = ArmorHider.GSON.fromJson(r, PresetStorage.class);
                    if (loaded != null) {
                        if (loaded.presets != null) {
                            for (int i = 0; i < Math.min(loaded.presets.length, PRESET_COUNT); i++) {
                                presets[i] = loaded.presets[i];
                            }
                        }
                        activeIndex = loaded.activeIndex;
                        if (activeIndex < -1 || activeIndex >= PRESET_COUNT) {
                            activeIndex = -1;
                        }
                        // Files written before the quick-toggle existed lack these fields; Gson then keeps
                        // the defaults from PresetStorage. Out-of-range values are healed the same way.
                        quickToggleA = isValidIndex(loaded.quickToggleA) ? loaded.quickToggleA : DEFAULT_QUICK_TOGGLE_A;
                        quickToggleB = isValidIndex(loaded.quickToggleB) ? loaded.quickToggleB : DEFAULT_QUICK_TOGGLE_B;
                        quickToggleKeyChecked = loaded.quickToggleKeyChecked;
                        quickToggleNoticePending = loaded.quickToggleNoticePending;
                    }
                }
                ArmorHider.LOGGER.info("Loaded {} presets from file (active: {}).", countNonNull(), activeIndex);
            } else {
                seedDefaults();
                save();
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to load presets, starting fresh.", e);
            seedDefaults();
        }
    }

    private void seedDefaults() {
        double[] opacities = {0.0, 0.2, 0.4, 0.6, 0.8};
        for (int i = 0; i < PRESET_COUNT; i++) {
            presets[i] = ConfigPreset.withUniformOpacity(opacities[i]);
        }
        activeIndex = -1;
        ArmorHider.LOGGER.info("Seeded {} default presets.", PRESET_COUNT);
    }

    private void save() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer w = Files.newBufferedWriter(file)) {
                var storage = new PresetStorage();
                storage.presets = presets.clone();
                storage.activeIndex = activeIndex;
                storage.quickToggleA = quickToggleA;
                storage.quickToggleB = quickToggleB;
                storage.quickToggleKeyChecked = quickToggleKeyChecked;
                storage.quickToggleNoticePending = quickToggleNoticePending;
                ArmorHider.GSON.toJson(storage, w);
            }
            // Cleared only after the write actually succeeds: clearing up front would make a failed save
            // silently discard the pending edits, since flushPendingSave() would never retry them.
            pendingSave = false;
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to save presets!", e);
        }
    }

    private int countNonNull() {
        int count = 0;
        for (var p : presets) if (p != null) count++;
        return count;
    }

    private static class PresetStorage {
        ConfigPreset @Nullable [] presets;
        int activeIndex = -1;
        int quickToggleA = DEFAULT_QUICK_TOGGLE_A;
        int quickToggleB = DEFAULT_QUICK_TOGGLE_B;
        boolean quickToggleKeyChecked;
        boolean quickToggleNoticePending;
    }
}
