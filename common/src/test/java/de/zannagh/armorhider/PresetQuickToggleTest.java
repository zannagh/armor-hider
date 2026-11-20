package de.zannagh.armorhider;

import de.zannagh.armorhider.configuration.PresetManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetQuickToggleTest {

    @TempDir
    Path tempDir;

    private PresetManager createManager() {
        return new PresetManager(tempDir.resolve("presets.json"));
    }

    @Test
    @DisplayName("defaults are preset 1 and preset 2")
    void defaults() {
        var manager = createManager();
        assertEquals(0, manager.getQuickToggleA());
        assertEquals(1, manager.getQuickToggleB());
    }

    @Test
    @DisplayName("activates B while A is active")
    void aActiveGoesToB() {
        var manager = createManager();
        manager.setActiveIndex(manager.getQuickToggleA());
        assertEquals(manager.getQuickToggleB(), manager.resolveQuickToggleTarget());
    }

    @Test
    @DisplayName("activates A when B, another preset or none is active")
    void otherwiseGoesToA() {
        var manager = createManager();
        manager.setQuickTogglePresets(2, 4);

        manager.setActiveIndex(4);
        assertEquals(2, manager.resolveQuickToggleTarget());
        manager.setActiveIndex(1);
        assertEquals(2, manager.resolveQuickToggleTarget());
        manager.deactivate();
        assertEquals(2, manager.resolveQuickToggleTarget());
    }

    @Test
    @DisplayName("alternates between A and B on repeated presses")
    void alternates() {
        var manager = createManager();
        manager.setQuickTogglePresets(3, 0);
        manager.deactivate();

        manager.setActiveIndex(manager.resolveQuickToggleTarget());
        assertEquals(3, manager.getActiveIndex());
        manager.setActiveIndex(manager.resolveQuickToggleTarget());
        assertEquals(0, manager.getActiveIndex());
        manager.setActiveIndex(manager.resolveQuickToggleTarget());
        assertEquals(3, manager.getActiveIndex());
    }

    @Test
    @DisplayName("A equal to B always resolves to that preset")
    void sameForBoth() {
        var manager = createManager();
        manager.setQuickTogglePresets(2, 2);
        manager.setActiveIndex(2);
        assertEquals(2, manager.resolveQuickToggleTarget());
    }

    @Test
    @DisplayName("ignores out-of-range selections")
    void rejectsInvalid() {
        var manager = createManager();
        manager.setQuickTogglePresets(3, 4);
        manager.setQuickTogglePresets(-1, 2);
        manager.setQuickTogglePresets(1, PresetManager.PRESET_COUNT);
        assertEquals(3, manager.getQuickToggleA());
        assertEquals(4, manager.getQuickToggleB());
    }

    @Test
    @DisplayName("selection survives a reload")
    void persists() {
        createManager().setQuickTogglePresets(4, 2);
        var reloaded = createManager();
        assertEquals(4, reloaded.getQuickToggleA());
        assertEquals(2, reloaded.getQuickToggleB());
    }

    @Test
    @DisplayName("existing preset files without the fields migrate to the defaults")
    void legacyFileMigrates() throws IOException {
        Files.writeString(tempDir.resolve("presets.json"), "{\"activeIndex\":2}");
        var manager = createManager();
        assertEquals(2, manager.getActiveIndex());
        assertEquals(PresetManager.DEFAULT_QUICK_TOGGLE_A, manager.getQuickToggleA());
        assertEquals(PresetManager.DEFAULT_QUICK_TOGGLE_B, manager.getQuickToggleB());
    }

    @Test
    @DisplayName("out-of-range stored values heal to the defaults")
    void corruptValuesHeal() throws IOException {
        Files.writeString(tempDir.resolve("presets.json"), "{\"quickToggleA\":9,\"quickToggleB\":-3}");
        var manager = createManager();
        assertEquals(PresetManager.DEFAULT_QUICK_TOGGLE_A, manager.getQuickToggleA());
        assertEquals(PresetManager.DEFAULT_QUICK_TOGGLE_B, manager.getQuickToggleB());
    }

    @Test
    @DisplayName("key check and owed notice default to off and survive a reload")
    void keyStatePersists() {
        var manager = createManager();
        assertFalse(manager.isQuickToggleKeyChecked());
        assertFalse(manager.isQuickToggleNoticePending());

        manager.setQuickToggleKeyState(true, true);
        var reloaded = createManager();
        assertTrue(reloaded.isQuickToggleKeyChecked());
        assertTrue(reloaded.isQuickToggleNoticePending());

        reloaded.setQuickToggleKeyState(true, false);
        var again = createManager();
        assertTrue(again.isQuickToggleKeyChecked());
        assertFalse(again.isQuickToggleNoticePending());
    }
}
