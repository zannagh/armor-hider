package de.zannagh.armorhider.configuration;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class PresetManager {

    public static final int PRESET_COUNT = 5;
    private static final Path DEFAULT_FILE = new File("config", "armor-hider-presets.json").toPath();

    private final Path file;
    private final ConfigPreset @Nullable [] presets = new ConfigPreset[PRESET_COUNT];
    private int activeIndex = -1;

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

    public void updateActivePreset(PlayerConfig config) {
        if (activeIndex >= 0 && activeIndex < PRESET_COUNT && presets[activeIndex] != null) {
            presets[activeIndex] = ConfigPreset.fromPlayerConfig(config);
            save();
        }
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
                ArmorHider.GSON.toJson(storage, w);
            }
        } catch (IOException e) {
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
    }
}
