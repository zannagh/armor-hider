package de.zannagh.armorhider.api.compat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The compat-flag registry. {@link CompatFlags#isAvailable} takes the class-presence probe as a
 * parameter, so the whole detection contract is unit-testable without a running class loader.
 */
@DisplayName("CompatFlags detection contract")
class CompatFlagsTest {

    @Test
    @DisplayName("isAvailable is true when the probe reports any of the flag's classes present")
    void availableWhenAnyClassPresent() {
        // EMF carries a single class name; make the probe say it is loaded.
        assertTrue(CompatFlags.ENTITY_MODEL_FEATURES.isAvailable(name -> true));
    }

    @Test
    @DisplayName("isAvailable is false when the probe reports none of the flag's classes present")
    void unavailableWhenNoClassPresent() {
        for (CompatFlags flag : CompatFlags.values()) {
            assertFalse(flag.isAvailable(name -> false), () -> flag + " must be absent when nothing probes true");
        }
    }

    @Test
    @DisplayName("isAvailable probes exactly the flag's own class names")
    void availableProbesOwnClassNamesOnly() {
        // A probe that only returns true for ElytraTrims' entrypoint must light up ET and nothing else.
        String etEntrypoint = "dev.kikugie.elytratrims.ep.ETClientEntrypoint";
        for (CompatFlags flag : CompatFlags.values()) {
            boolean expected = flag == CompatFlags.ELYTRA_TRIMS;
            assertTrue(expected == flag.isAvailable(etEntrypoint::equals),
                    () -> flag + " availability against the ET-only probe was wrong");
        }
    }

    @Test
    @DisplayName("only EMF and Iris require initialization")
    void onlyEmfAndIrisNeedInitialization() {
        Set<CompatFlags> needInit = EnumSet.noneOf(CompatFlags.class);
        for (CompatFlags flag : CompatFlags.values()) {
            if (flag.needsInitialization()) {
                needInit.add(flag);
            }
        }
        assertTrue(needInit.contains(CompatFlags.ENTITY_MODEL_FEATURES));
        assertTrue(needInit.contains(CompatFlags.IRIS));
        assertTrue(EnumSet.of(CompatFlags.ENTITY_MODEL_FEATURES, CompatFlags.IRIS).containsAll(needInit),
                "no flag other than EMF/Iris should require initialization");
    }

    @Test
    @DisplayName("every flag probes at least one non-blank class name")
    void everyFlagHasAClassName() {
        for (CompatFlags flag : CompatFlags.values()) {
            // A blank probe target would make the flag impossible to detect; drive isAvailable with a
            // probe that only accepts non-blank strings and require it to be reachable.
            assertTrue(flag.isAvailable(name -> name != null && !name.isBlank()),
                    () -> flag + " has no usable class name to probe");
        }
    }
}
