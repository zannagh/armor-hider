package de.zannagh.armorhider.client.render;

//? if >= 26.4-0.snapshot.1 {
/*import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Bookkeeping behind ImageTextures on 26.4: the GPU side (TextureResources / TextureManager) is replaced
// by string handles and recorded calls, so ownership, close order and failure paths are checked here.
@DisplayName("OwnedTextureRegistry ownership of 26.4 texture resources")
class OwnedTextureRegistryTest {

    private final List<String> events = new ArrayList<>();
    private final OwnedTextureRegistry<String, String> registry =
            new OwnedTextureRegistry<>(value -> events.add("close " + value));

    private void register(String key, String value) {
        registry.register(key, value, v -> events.add("register " + key + "=" + v));
    }

    @Test
    @DisplayName("register keeps the value open and owned")
    void registerOwnsValue() {
        register("a", "tex1");
        assertEquals(List.of("register a=tex1"), events);
        assertEquals(1, registry.size());
    }

    @Test
    @DisplayName("release unregisters first, then closes the owned value")
    void releaseClosesOwnedValue() {
        register("a", "tex1");
        events.clear();
        registry.release("a", key -> events.add("release " + key));
        assertEquals(List.of("release a", "close tex1"), events);
        assertEquals(0, registry.size());
    }

    @Test
    @DisplayName("re-register of a key closes the replaced value after registering the new one")
    void reRegisterClosesPrevious() {
        register("a", "tex1");
        register("a", "tex2");
        assertEquals(List.of("register a=tex1", "register a=tex2", "close tex1"), events);
        assertEquals(1, registry.size());
        events.clear();
        registry.release("a", key -> { });
        assertEquals(List.of("close tex2"), events);
    }

    @Test
    @DisplayName("a failing registrar closes the new value and keeps the previous one owned")
    void failedRegisterClosesNewValue() {
        register("a", "tex1");
        events.clear();
        RuntimeException boom = new RuntimeException("boom");
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> registry.register("a", "tex2", v -> { throw boom; }));
        assertSame(boom, thrown);
        assertEquals(List.of("close tex2"), events);
        events.clear();
        registry.release("a", key -> { });
        assertEquals(List.of("close tex1"), events);
    }

    @Test
    @DisplayName("release of an unknown key still reaches the releaser and closes nothing")
    void releaseUnknownKey() {
        registry.release("missing", key -> events.add("release " + key));
        assertEquals(List.of("release missing"), events);
    }

    @Test
    @DisplayName("value is closed even if the releaser throws")
    void releaseClosesWhenReleaserThrows() {
        register("a", "tex1");
        events.clear();
        assertThrows(IllegalStateException.class,
                () -> registry.release("a", key -> { throw new IllegalStateException(); }));
        assertTrue(events.contains("close tex1"));
        assertEquals(0, registry.size());
    }
}
*///?}
