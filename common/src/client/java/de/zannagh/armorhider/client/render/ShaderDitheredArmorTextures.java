package de.zannagh.armorhider.client.render;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.IrisPartialTransparencyMode;import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

//? if >= 26.2-1.pre && < 26.3-0.snapshot.2 {
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.util.List;
//?}

/**
 * Screen-door (ordered-dither) "faded armor" textures for use under an active Iris shaderpack.
 * <p>
 * Under Iris, an alpha-blended (translucent) armor piece is routed into the shaderpack's
 * {@code gbuffers_entities_translucent} program, where Complementary (and most deferred packs)
 * composite it such that the OPAQUE body behind the piece reads see-through - the terrain/sky draws
 * straight through the torso (issue #342 follow-up). Depth-write vs no-depth makes no difference:
 * any alpha blend lands in that broken translucent pass.
 * <p>
 * The shader-safe way to fake partial opacity is to stay fully OPAQUE and instead punch holes: we
 * bake an ordered (Bayer) dither into a copy of the armor texture's alpha channel - keeping roughly
 * {@code opacity} of the originally-opaque texels and zeroing the rest - then render it on the normal
 * opaque {@code armorCutoutNoCull} type. The pack's cutout alpha-test discards the zeroed texels,
 * revealing the opaque body behind them; every surviving texel is fully opaque, so nothing enters the
 * translucent pass and the body never reads see-through. TAA blends the stipple into a smooth fade.
 * <p>
 * Generated lazily on the render thread, one {@link DynamicTexture} per (base texture, opacity bucket),
 * and cached. Only wired where the depth-writing-under-shaders era applies (>= 26.2-1.pre); elsewhere
 * {@link #ditheredTexture} returns {@code null} and callers keep their existing translucent path.
 */
public final class ShaderDitheredArmorTextures {

    private ShaderDitheredArmorTextures() {}

    // Number of opacity buckets. Opacity is snapped to a bucket so the cache stays small (at most this
    // many derived textures per armor material) while still giving a smooth-enough gradation.
    public static final int BUCKETS = 32;

    // Approximate native-memory budget for generated dither textures. Each entry is a full RGBA NativeImage
    // plus its GL texture (up to tens of MB under HD packs), so the cache is byte-budgeted, not count-limited.
    // Once the total exceeds this, least-recently-used entries are released until it fits again (issue #357).
    private static final long MAX_CACHE_BYTES = 1024L * 1024L * 1024L;

    // Access-ordered LRU of derived dither id -> approximate native byte size, guarded by CACHE_LOCK. Replaces
    // the former unbounded set that only ever released on a resource reload, which could accumulate many GB of
    // native textures across a session and exhaust off-heap memory.
    private static final Object CACHE_LOCK = new Object();
    private static final LinkedHashMap<Identifier, Long> CACHE = new LinkedHashMap<>(64, 0.75F, true);
    private static long cacheBytes = 0L;

    // Golden-ratio conjugate: a low-discrepancy per-phase offset so the PHASES thresholds spread evenly
    // across [0,1) (each pixel is kept in ~coverage of the phases, consecutive frames decorrelated).
    private static final float GOLDEN_CONJUGATE = 0.6180339887F;

    // Bumped once per level render (GameRendererMixin) to advance the temporal phase. Always present
    // (harmless off-gate) so the mixin can call it on every version.
    private static volatile int frameCounter = 0;

    public static void advanceFrame() {
        frameCounter++;
    }

    /**
     * Returns a dithered, opaque-cutout-safe copy of {@code base} whose alpha coverage approximates
     * {@code opacity}, or {@code null} if unavailable on this version / the base texture can't be read
     * (caller then falls back to its normal path).
     *
     * @param base    the armor texture identifier the piece would normally render with.
     * @param opacity desired visible opacity in (0,1).
     * @return the derived dithered texture identifier, or {@code null} to fall back.
     */
    public static Identifier ditheredTexture(Identifier base, float opacity, de.zannagh.armorhider.net.packets.PlayerConfig config) {
        //? if >= 26.2-1.pre && < 26.3-0.snapshot.2 {
        if (base == null) {
            return null;
        }
        // Drop cached dither textures when the resource stack changes (resource-pack swap / F3+T), so a
        // stale base texture isn't reused and the generated DynamicTextures don't accumulate forever.
        invalidateCacheIfNeeded();
        if (opacity >= 1.0F) {
            // Fully (or near-fully) opaque: no dither needed, caller should use the plain opaque type.
            return null;
        }
        int bucket = (int) Math.floor(opacity * BUCKETS);
        bucket = Math.max(1, Math.min(bucket, BUCKETS - 1));
        // Mode gates everything: NONE means the whole dithering fix is off, so bail out and let the
        // caller keep the (pre-fix) translucent path. DITHERING is a single static pattern (phase 0);
        // only TEMPORAL_DITHERING cycles the phase per frame so TAA can average the frames.
        IrisPartialTransparencyMode mode = config.irisPartialTransparencyMode.getValue();
        if (mode == IrisPartialTransparencyMode.NONE) {
            return null;
        }
        int phaseCount = Math.max(1, (int) config.irisDitheringPhases.getValue());
        int phase = mode == IrisPartialTransparencyMode.TEMPORAL_DITHERING
                ? Math.floorMod(frameCounter, phaseCount)
                : 0;
        Identifier derived = Identifier.fromNamespaceAndPath("armor_hider",
                "dither/" + bucket + "/p" + phase + "/" + base.getNamespace() + "/" + base.getPath());
        synchronized (CACHE_LOCK) {
            // get() on the access-ordered map both checks presence and marks the entry most-recently-used.
            if (CACHE.get(derived) != null) {
                return derived;
            }
        }
        // Track the native resources so a throw anywhere below (e.g. a NativeImage allocation failing under
        // the very memory pressure this class guards against) frees them in finally instead of leaking.
        NativeImage source = null;
        NativeImage image = null;
        DynamicTexture texture = null;
        try {
            source = readBaseTexture(base);
            if (source == null) {
                return null;
            }
            float phaseOffset = (phase * GOLDEN_CONJUGATE) % 1.0F;
            image = buildDithered(source, (float) bucket / BUCKETS, phaseOffset, config);
            source.close();
            source = null;
            long bytes = (long) image.getWidth() * image.getHeight() * 4L;
            texture = new DynamicTexture(derived::toString, image);
            // DynamicTexture now owns the image and closes it on texture.close(); drop our handle so the
            // finally block never double-closes it.
            image = null;
            synchronized (CACHE_LOCK) {
                if (CACHE.containsKey(derived)) {
                    // Lost a race: another thread already built and registered this id. Drop our copy
                    // (frees the NativeImage + GL texture) rather than leaking it or double-registering.
                    texture.close();
                    texture = null;
                    return derived;
                }
                Minecraft.getInstance().getTextureManager().register(derived, texture);
                CACHE.put(derived, bytes);
                cacheBytes += bytes;
                // Ownership has passed to the TextureManager / cache; don't let finally close it.
                texture = null;
                evictIfOverBudget();
            }
            return derived;
        } catch (Throwable t) {
            ArmorHider.LOGGER.warn("[armor-hider] failed to build dithered armor texture for {} ({})",
                    base, t.toString());
            return null;
        } finally {
            if (source != null) {
                source.close();
            }
            if (image != null) {
                image.close();
            }
            if (texture != null) {
                texture.close();
            }
        }
        //?} else {
        /*return null;
        *///?}
    }

    //? if >= 26.2-1.pre && < 26.3-0.snapshot.2 {
    // The resource stack in effect when the current cache entries were built. A resource reload swaps
    // the ResourceManager instance, so an identity change signals that cached textures are stale.
    private static ResourceManager lastResourceManager;

    // Releases least-recently-used dither textures (freeing each NativeImage + GL texture) until the cache
    // fits within MAX_CACHE_BYTES. The caller must hold CACHE_LOCK; access-order iteration visits LRU first.
    private static void evictIfOverBudget() {
        if (cacheBytes <= MAX_CACHE_BYTES) {
            return;
        }
        var textureManager = Minecraft.getInstance().getTextureManager();
        var iterator = CACHE.entrySet().iterator();
        int evicted = 0;
        while (cacheBytes > MAX_CACHE_BYTES && iterator.hasNext()) {
            Map.Entry<Identifier, Long> eldest = iterator.next();
            textureManager.release(eldest.getKey());
            cacheBytes -= eldest.getValue();
            iterator.remove();
            evicted++;
        }
        if (evicted > 0) {
            ArmorHider.LOGGER.debug("[armor-hider] dither cache over budget - released {} LRU textures ({} MB now cached)",
                    evicted, cacheBytes / (1024L * 1024L));
        }
    }

    private static void invalidateCacheIfNeeded() {
        ResourceManager current = Minecraft.getInstance().getResourceManager();
        if (current == lastResourceManager) {
            return;
        }
        synchronized (CACHE_LOCK) {
            if (lastResourceManager != null && !CACHE.isEmpty()) {
                var textureManager = Minecraft.getInstance().getTextureManager();
                for (Identifier id : CACHE.keySet()) {
                    textureManager.release(id);
                }
                ArmorHider.LOGGER.debug("[armor-hider] resource reload - released {} cached dither textures",
                        CACHE.size());
            }
            CACHE.clear();
            cacheBytes = 0L;
        }
        lastResourceManager = current;
    }

    private static NativeImage readBaseTexture(Identifier base) throws Exception {
        List<Resource> stack = Minecraft.getInstance().getResourceManager().getResourceStack(base);
        if (stack.isEmpty()) {
            return null;
        }
        // Last entry = highest-priority pack (respects resource packs / ETF overrides where present).
        try (InputStream in = stack.get(stack.size() - 1).open()) {
            return NativeImage.read(in);
        }
    }

    // Builds a DITHER_SCALE-upscaled copy of {@code source} (nearest-neighbour, so the art stays blocky)
    // and applies the screen-door dither in the UPSCALED space: within each original texel's
    // SCALE x SCALE block the threshold is evaluated per output pixel, so the grain cells are 1/SCALE of
    // a texel instead of a whole texel. {@code phaseOffset} shifts the IGN threshold for this frame's
    // temporal phase so a different (decorrelated) ~coverage subset is kept each frame. Keeps ~coverage
    // of the originally-opaque texels and zeroes the alpha of the rest so the cutout alpha-test discards
    // them; already-transparent texels are copied through untouched. NativeImage packs ABGR (alpha top).
    private static NativeImage buildDithered(NativeImage source, float coverage, float phaseOffset, de.zannagh.armorhider.net.packets.PlayerConfig config) {
        int w = source.getWidth();
        int h = source.getHeight();
        // Cap the generated texture at 4096px on its longest side so an HD resource-pack armor texture
        // can't blow up memory; vanilla 64px art keeps the full DITHER_SCALE.
        int scale = Math.max(1, (int)config.irisDitheringScale.getValue());
        while (scale > 1 && Math.max(w, h) * scale > (int)config.irisDitheringResCap.getValue()) {
            scale--;
        }
        NativeImage out = new NativeImage(w * scale, h * scale, false);
        for (int sy = 0; sy < h; sy++) {
            for (int sx = 0; sx < w; sx++) {
                int argb = source.getPixel(sx, sy);
                int alpha = (argb >>> 24) & 0xFF;
                boolean alreadyTransparent = alpha < 26; // below the ~0.1 cutout ref
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        int x = sx * scale + dx;
                        int y = sy * scale + dy;
                        if (alreadyTransparent) {
                            out.setPixel(x, y, argb);
                            continue;
                        }
                        float threshold = interleavedGradientNoise(x, y) + phaseOffset;
                        if (threshold >= 1.0F) {
                            threshold -= 1.0F; // wrap into [0,1)
                        }
                        // zero alpha -> discarded by the cutout test; else keep the opaque texel
                        out.setPixel(x, y, coverage <= threshold ? (argb & 0x00FFFFFF) : argb);
                    }
                }
            }
        }
        return out;
    }

    // Interleaved gradient noise (Jimenez) - a cheap blue-noise-like hash in [0,1). Unlike an ordered
    // Bayer matrix it has no visible grid/checker structure, so the screen door reads as fine organic
    // grain and looks much smoother at a given resolution.
    private static float interleavedGradientNoise(int x, int y) {
        float f = 0.06711056F * x + 0.00583715F * y;
        f = 52.9829189F * (f - (float) Math.floor(f));
        return f - (float) Math.floor(f);
    }
    //?}
}
