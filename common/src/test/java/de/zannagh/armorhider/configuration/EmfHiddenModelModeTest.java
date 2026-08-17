package de.zannagh.armorhider.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link EmfHiddenModelMode#fromName(String)} must never throw for a hand-edited or forward-version
 * config: anything it does not recognise resolves to {@link EmfHiddenModelMode#KEEP}.
 */
@DisplayName("EmfHiddenModelMode.fromName resolution")
class EmfHiddenModelModeTest {

    @Test
    @DisplayName("each canonical name resolves to its own mode")
    void resolvesCanonicalNames() {
        for (EmfHiddenModelMode mode : EmfHiddenModelMode.values()) {
            assertSame(mode, EmfHiddenModelMode.fromName(mode.name()));
        }
    }

    @Test
    @DisplayName("resolution is case-insensitive")
    void resolvesCaseInsensitively() {
        assertSame(EmfHiddenModelMode.VANILLA, EmfHiddenModelMode.fromName("vanilla"));
        assertSame(EmfHiddenModelMode.VANILLA_SEAMS, EmfHiddenModelMode.fromName("Vanilla_Seams"));
        assertSame(EmfHiddenModelMode.KEEP, EmfHiddenModelMode.fromName("keep"));
    }

    @Test
    @DisplayName("null falls back to KEEP")
    void nullFallsBackToKeep() {
        assertSame(EmfHiddenModelMode.KEEP, EmfHiddenModelMode.fromName(null));
    }

    @Test
    @DisplayName("an unknown or forward-version name falls back to KEEP")
    void unknownFallsBackToKeep() {
        assertSame(EmfHiddenModelMode.KEEP, EmfHiddenModelMode.fromName("SOME_FUTURE_MODE"));
        assertSame(EmfHiddenModelMode.KEEP, EmfHiddenModelMode.fromName(""));
        assertSame(EmfHiddenModelMode.KEEP, EmfHiddenModelMode.fromName("   "));
    }

    @Test
    @DisplayName("KEEP is the default first constant")
    void keepIsDefault() {
        assertEquals(EmfHiddenModelMode.KEEP, EmfHiddenModelMode.values()[0]);
    }
}
