package de.zannagh.armorhider.paper.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The name shared-rule state is keyed by, which has to come out byte-identical to what a client
 * computes for the same player - otherwise every client looks the state up under a name the server
 * never used and shared rules silently do nothing on that server.
 *
 * <p>Only the pure half is exercised here: the Bukkit lookup in {@code DisplayNames#of} needs a live
 * server, and the plugin's test suite deliberately runs without a Bukkit mock.</p>
 */
@DisplayName("DisplayNames team decoration")
class DisplayNamesTest {

    @Test
    @DisplayName("an undecorated player keeps their profile name")
    void undecoratedNameIsUnchanged() {
        assertEquals("Zannagh", DisplayNames.decorate("Zannagh", "", ""));
    }

    @Test
    @DisplayName("a team prefix and suffix wrap the name, matching how the client flattens them")
    void prefixAndSuffixWrapTheName() {
        assertEquals("[VIP] Zannagh!", DisplayNames.decorate("Zannagh", "[VIP] ", "!"));
    }

    @Test
    @DisplayName("legacy colour codes are stripped - they are styling on the client, not text")
    void legacyColourCodesAreStripped() {
        // Bukkit's legacy Team#getPrefix() serialises the styling into the string; the client's
        // Component#getString() keeps only the text, so leaving the codes in would mismatch on exactly
        // the coloured-team servers this decoration exists for.
        assertEquals("[VIP] Zannagh", DisplayNames.decorate("Zannagh", "§c[VIP]§r ", ""));
    }

    @Test
    @DisplayName("every legacy code letter is recognised, in either case")
    void allLegacyCodesAreRecognised() {
        assertEquals("x", DisplayNames.decorate("x",
                "§0§9§a§f§k§o§r", ""));
        assertEquals("x", DisplayNames.decorate("x", "§A§F§K§R", ""));
    }

    @Test
    @DisplayName("a lone section sign is left alone rather than eating the next character")
    void loneSectionSignSurvives() {
        // "§z" is not a formatting code, so it is literal text on the client too.
        assertEquals("§zZannagh", DisplayNames.decorate("Zannagh", "§z", ""));
    }

    @Test
    @DisplayName("a null prefix or suffix is treated as absent")
    void nullAffixesAreEmpty() {
        assertEquals("Zannagh", DisplayNames.decorate("Zannagh", null, null));
    }
}
