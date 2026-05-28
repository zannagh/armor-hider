package de.zannagh.armorhider;

import de.zannagh.armorhider.configuration.ConfigPreset;
import de.zannagh.armorhider.configuration.PresetManager;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigPresetTests {

    @Nested
    @DisplayName("fromPlayerConfig")
    class FromPlayerConfig {

        @Test
        @DisplayName("captures all preset-eligible fields")
        void capturesAllFields() {
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            config.helmetOpacity.setValue(0.5);
            config.chestOpacity.setValue(0.3);
            config.legsOpacity.setValue(0.7);
            config.bootsOpacity.setValue(0.9);
            config.offHandOpacity.setValue(0.4);
            config.helmetGlint.setValue(false);
            config.chestGlint.setValue(false);
            config.legsGlint.setValue(true);
            config.bootsGlint.setValue(true);
            config.opacityAffectingHatOrSkull.setValue(false);
            config.opacityAffectingElytra.setValue(true);
            config.enableCombatDetection.setValue(true);
            config.inCombatUseDefaultModel.setValue(true);

            var preset = ConfigPreset.fromPlayerConfig(config);

            assertEquals(0.5, preset.helmetOpacity);
            assertEquals(0.3, preset.chestOpacity);
            assertEquals(0.7, preset.legsOpacity);
            assertEquals(0.9, preset.bootsOpacity);
            assertEquals(0.4, preset.offHandOpacity);
            assertFalse(preset.helmetGlint);
            assertFalse(preset.chestGlint);
            assertTrue(preset.legsGlint);
            assertTrue(preset.bootsGlint);
            assertFalse(preset.opacityAffectingHatOrSkull);
            assertTrue(preset.opacityAffectingElytra);
            assertTrue(preset.enableCombatDetection);
            assertTrue(preset.inCombatUseDefaultModel);
        }
    }

    @Nested
    @DisplayName("applyTo")
    class ApplyTo {

        @Test
        @DisplayName("applies all preset fields to a config")
        void appliesAllFields() {
            var preset = new ConfigPreset();
            preset.helmetOpacity = 0.2;
            preset.chestOpacity = 0.4;
            preset.legsOpacity = 0.6;
            preset.bootsOpacity = 0.8;
            preset.offHandOpacity = 0.1;
            preset.helmetGlint = false;
            preset.chestGlint = true;
            preset.legsGlint = false;
            preset.bootsGlint = true;
            preset.opacityAffectingHatOrSkull = false;
            preset.opacityAffectingElytra = false;

            var config = new PlayerConfig(UUID.randomUUID(), "test");
            preset.applyTo(config);

            assertEquals(0.2, config.helmetOpacity.getValue());
            assertEquals(0.4, config.chestOpacity.getValue());
            assertEquals(0.6, config.legsOpacity.getValue());
            assertEquals(0.8, config.bootsOpacity.getValue());
            assertEquals(0.1, config.offHandOpacity.getValue());
            assertFalse(config.helmetGlint.getValue());
            assertTrue(config.chestGlint.getValue());
            assertFalse(config.legsGlint.getValue());
            assertTrue(config.bootsGlint.getValue());
            assertFalse(config.opacityAffectingHatOrSkull.getValue());
            assertFalse(config.opacityAffectingElytra.getValue());
        }

        @Test
        @DisplayName("does not affect non-preset fields")
        void doesNotAffectNonPresetFields() {
            var preset = new ConfigPreset();
            preset.helmetOpacity = 0.0;

            var config = new PlayerConfig(UUID.randomUUID(), "test");
            config.disableArmorHider.setValue(true);
            config.disableArmorHiderForOthers.setValue(true);
            config.showSettingsInSkinCustomization.setValue(true);

            preset.applyTo(config);

            assertTrue(config.disableArmorHider.getValue());
            assertTrue(config.disableArmorHiderForOthers.getValue());
            assertTrue(config.showSettingsInSkinCustomization.getValue());
        }
    }

    @Nested
    @DisplayName("matchesPlayerConfig")
    class Matches {

        @Test
        @DisplayName("matches when all preset fields are equal")
        void matchesWhenEqual() {
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            var preset = ConfigPreset.fromPlayerConfig(config);

            assertTrue(preset.matchesPlayerConfig(config));
        }

        @Test
        @DisplayName("does not match when any field differs")
        void doesNotMatchWhenDifferent() {
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            var preset = ConfigPreset.fromPlayerConfig(config);
            config.helmetOpacity.setValue(0.5);

            assertFalse(preset.matchesPlayerConfig(config));
        }

        @Test
        @DisplayName("ignores non-preset fields for matching")
        void ignoresNonPresetFields() {
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            var preset = ConfigPreset.fromPlayerConfig(config);
            config.disableArmorHider.setValue(!config.disableArmorHider.getValue());

            assertTrue(preset.matchesPlayerConfig(config));
        }

        @Test
        @DisplayName("does not match when combat detection differs")
        void doesNotMatchWhenCombatDetectionDiffers() {
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            var preset = ConfigPreset.fromPlayerConfig(config);
            config.enableCombatDetection.setValue(!config.enableCombatDetection.getValue());

            assertFalse(preset.matchesPlayerConfig(config));
        }
    }

    @Nested
    @DisplayName("deepCopy")
    class DeepCopy {

        @Test
        @DisplayName("creates independent copy")
        void createsIndependentCopy() {
            var original = new ConfigPreset();
            original.helmetOpacity = 0.5;

            var copy = original.deepCopy();
            copy.helmetOpacity = 0.9;

            assertEquals(0.5, original.helmetOpacity);
            assertEquals(0.9, copy.helmetOpacity);
        }
    }

    @Nested
    @DisplayName("roundtrip")
    class Roundtrip {

        @Test
        @DisplayName("from config and back preserves values")
        void roundtrip() {
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            config.helmetOpacity.setValue(0.3);
            config.chestGlint.setValue(false);
            config.opacityAffectingElytra.setValue(false);

            var preset = ConfigPreset.fromPlayerConfig(config);

            var freshConfig = new PlayerConfig(UUID.randomUUID(), "other");
            preset.applyTo(freshConfig);

            assertEquals(config.helmetOpacity.getValue(), freshConfig.helmetOpacity.getValue());
            assertEquals(config.chestGlint.getValue(), freshConfig.chestGlint.getValue());
            assertEquals(config.opacityAffectingElytra.getValue(), freshConfig.opacityAffectingElytra.getValue());

            assertTrue(preset.matchesPlayerConfig(freshConfig));
        }
    }

    @Nested
    @DisplayName("PresetManager active index")
    class ActiveIndex {

        @TempDir
        Path tempDir;

        private PresetManager createManager() {
            return new PresetManager(tempDir.resolve("presets.json"));
        }

        @Test
        @DisplayName("starts with no active preset")
        void startsInactive() {
            var manager = createManager();
            assertEquals(-1, manager.getActiveIndex());
            assertFalse(manager.isActive(0));
        }

        @Test
        @DisplayName("setActiveIndex makes that preset active")
        void setActive() {
            var manager = createManager();
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            manager.saveFromCurrentConfig(2, config);
            manager.setActiveIndex(2);

            assertEquals(2, manager.getActiveIndex());
            assertTrue(manager.isActive(2));
            assertFalse(manager.isActive(0));
        }

        @Test
        @DisplayName("deactivate clears active index")
        void deactivate() {
            var manager = createManager();
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            manager.saveFromCurrentConfig(1, config);
            manager.setActiveIndex(1);
            manager.deactivate();

            assertEquals(-1, manager.getActiveIndex());
            assertFalse(manager.isActive(1));
        }

        @Test
        @DisplayName("updateActivePreset updates the active preset's values")
        void updateActive() {
            var manager = createManager();
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            config.helmetOpacity.setValue(0.5);
            manager.saveFromCurrentConfig(0, config);
            manager.setActiveIndex(0);

            config.helmetOpacity.setValue(0.8);
            manager.updateActivePreset(config);

            assertEquals(0.8, manager.getPreset(0).helmetOpacity);
        }

        @Test
        @DisplayName("updateActivePreset does nothing when no preset is active")
        void updateInactiveDoesNothing() {
            var manager = createManager();
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            config.helmetOpacity.setValue(0.5);
            manager.saveFromCurrentConfig(0, config);

            config.helmetOpacity.setValue(0.8);
            manager.updateActivePreset(config);

            assertEquals(0.5, manager.getPreset(0).helmetOpacity);
        }

        @Test
        @DisplayName("seeds 5 default presets with graduated opacities")
        void seedsDefaults() {
            var manager = createManager();
            double[] expected = {0.0, 0.2, 0.4, 0.6, 0.8};
            for (int i = 0; i < 5; i++) {
                assertTrue(manager.hasPreset(i), "Preset " + i + " should exist");
                var preset = manager.getPreset(i);
                assertEquals(expected[i], preset.helmetOpacity, "Preset " + i + " helmet");
                assertEquals(expected[i], preset.chestOpacity, "Preset " + i + " chest");
                assertEquals(expected[i], preset.legsOpacity, "Preset " + i + " legs");
                assertEquals(expected[i], preset.bootsOpacity, "Preset " + i + " boots");
                assertEquals(expected[i], preset.offHandOpacity, "Preset " + i + " offhand");
                assertTrue(preset.helmetGlint, "Preset " + i + " glint should be default");
                assertFalse(preset.enableCombatDetection, "Preset " + i + " combat detection should be default");
                assertFalse(preset.inCombatUseDefaultModel, "Preset " + i + " vanilla override should be default");
            }
        }

        @Test
        @DisplayName("active index persists across reload")
        void persistsActiveIndex() {
            var file = tempDir.resolve("persist-test.json");
            var manager1 = new PresetManager(file);
            var config = new PlayerConfig(UUID.randomUUID(), "test");
            manager1.saveFromCurrentConfig(3, config);
            manager1.setActiveIndex(3);

            var manager2 = new PresetManager(file);
            assertEquals(3, manager2.getActiveIndex());
            assertTrue(manager2.hasPreset(3));
        }
    }
}
