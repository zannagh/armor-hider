package de.zannagh.armorhider.api.compat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mixin-safe presence probes in {@link CompatManager}. Only the stateless {@code classExists} /
 * {@code isModPresent} helpers are exercised here - deliberately not the static flag-set mutators,
 * which would bleed global state into the shared test JVM.
 */
@DisplayName("CompatManager class/mod presence probing")
class CompatManagerProbeTest {

    @Test
    @DisplayName("classExists is true for a real class and false for an absent one")
    void classExistsBasics() {
        ClassLoader empty = new FakeResourceLoader(Set.of());
        assertTrue(CompatManager.classExists("java.lang.String", empty));
        assertFalse(CompatManager.classExists("de.zannagh.armorhider.NoSuchClass$Nope", empty));
    }

    @Test
    @DisplayName("classExists falls back to a resource probe when the class cannot be linked")
    void classExistsFallsBackToResourceProbe() {
        // Not loadable (no such class), but its .class resource is present -> the resource fallback wins.
        ClassLoader loader = new FakeResourceLoader(Set.of("com/example/mod/Entry.class"));
        assertTrue(CompatManager.classExists("com.example.mod.Entry", loader));
    }

    @Test
    @DisplayName("isModPresent detects a mod by its exact .class resource")
    void isModPresentByClassResource() {
        ClassLoader loader = new FakeResourceLoader(Set.of("com/example/mod/Entry.class"));
        assertTrue(CompatManager.isModPresent(loader, "com.example.mod.Entry"));
    }

    @Test
    @DisplayName("isModPresent falls back to the package directory when the class was renamed")
    void isModPresentByPackageFallback() {
        // The exact class is gone, but the package directory still resolves (renamed entrypoint case).
        ClassLoader loader = new FakeResourceLoader(Set.of("com/example/mod/"));
        assertTrue(CompatManager.isModPresent(loader, "com.example.mod.RenamedEntry"));
    }

    @Test
    @DisplayName("isModPresent is false when neither the class nor its package resolves")
    void isModPresentAbsent() {
        ClassLoader loader = new FakeResourceLoader(Set.of("some/other/pkg/"));
        assertFalse(CompatManager.isModPresent(loader, "com.example.mod.Entry"));
    }

    @Test
    @DisplayName("isModPresent is false for a package-less class name that is absent")
    void isModPresentTopLevelAbsent() {
        ClassLoader loader = new FakeResourceLoader(Set.of());
        assertFalse(CompatManager.isModPresent(loader, "TopLevelNoPackage"));
    }

    /** A classloader that exposes exactly the given resource paths and nothing else. */
    private static final class FakeResourceLoader extends ClassLoader {
        private final Set<String> resources;

        FakeResourceLoader(Set<String> resources) {
            super(null);
            this.resources = resources;
        }

        @Override
        public URL getResource(String name) {
            return resources.contains(name) ? stub(name) : null;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return resources.contains(name)
                    ? Collections.enumeration(List.of(stub(name)))
                    : Collections.emptyEnumeration();
        }

        private static URL stub(String name) {
            try {
                return URI.create("file:///fake/" + name).toURL();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
