package de.zannagh.armorhider.client.render;

//? if >= 26.4-0.snapshot.1 {
/*import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// Owns one closeable resource per key on 26.4, where TextureManager no longer frees what it is given:
// ExternalTextureProvider.releaseTexture is a no-op, so the caller keeps the TextureResources and closes
// them itself (vanilla's SkinTextureDownloader keeps the same per-Identifier map). Generic so the
// bookkeeping is unit-testable without a GPU device.
// (Line comments on purpose: the class body sits inside a stonecutter block comment before 26.4.)
final class OwnedTextureRegistry<K, V> {

    private final Map<K, V> owned = new HashMap<>();
    private final Consumer<V> closer;

    OwnedTextureRegistry(Consumer<V> closer) {
        this.closer = closer;
    }

    // Hands value to registrar and takes ownership of it. If registrar throws, value is closed and the
    // previous registration (if any) stays owned. Otherwise a value previously owned under key is closed
    // after the registrar replaced it, matching SkinTextureDownloader's register-then-close order.
    synchronized void register(K key, V value, Consumer<V> registrar) {
        try {
            registrar.accept(value);
        } catch (Throwable t) {
            closer.accept(value);
            throw t;
        }
        V previous = owned.put(key, value);
        if (previous != null && previous != value) {
            closer.accept(previous);
        }
    }

    // Unregisters key through releaser, then closes the value owned under it (if any). The releaser is
    // invoked even for unknown keys, so callers can route every release through here. If the releaser
    // throws, the value stays owned: TextureManager may still map key to it, so closing would leave a
    // closed texture in use.
    synchronized void release(K key, Consumer<K> releaser) {
        releaser.accept(key);
        V value = owned.remove(key);
        if (value != null) {
            closer.accept(value);
        }
    }

    synchronized int size() {
        return owned.size();
    }
}
*///?}
