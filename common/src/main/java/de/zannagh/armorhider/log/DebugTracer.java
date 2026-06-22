package de.zannagh.armorhider.log;

import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

/**
 * Structured trace-point methods for the rendering pipeline.
 * Each method is a thin wrapper around {@link DebugLogger} that formats
 * a human-readable message for a specific pipeline event.
 * <p>
 * To avoid log flood at 60+ FPS, messages are sampled: only logged
 * every {@value #LOG_EVERY_N_FRAMES}th frame.
 */
public final class DebugTracer {

    private static final int LOG_EVERY_N_FRAMES = 60;
    private static long frameCounter = 0;

    private DebugTracer() {}

    private static boolean shouldLogThisFrame() {
        return frameCounter % LOG_EVERY_N_FRAMES == 0;
    }

    public static void scopeEnterLevelRender() {
        frameCounter++;
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log(">> enterLevelRender (frame #{})", frameCounter);
    }

    public static void scopeExitLevelRender() {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("  << exitLevelRender");
    }

    public static void scopeEnterEntityRender() {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("    >> enterEntityRender (SENTINEL)");
    }

    public static void scopeExitEntityRender() {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("    << exitEntityRender");
    }

    public static void scopeEnterItemRender(EquipmentSlot slot, @Nullable String playerName, double transparency) {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("      >> enterItemRender: slot={}, player={}, transparency={}", slot, playerName, transparency);
    }

    public static void scopeExitItemRender() {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("      << exitItemRender");
    }

    // --- Per-render-scope lifecycle (not frame-sampled: scope churn is bounded,
    //     and missing an event makes debugging harder than a slightly busier log) ---

    public static void scopeEntered(String scope, @Nullable String identity, boolean hasModification) {
        if (!DebugLogger.isEnabled()) {
            return;
        }
        DebugLogger.log("      >> scope ENTER: {} | identity={} | hasModification={}",
                scope, identity != null ? identity : "<none>", hasModification);
    }

    public static void scopeExited(String scope, @Nullable String identity) {
        if (!DebugLogger.isEnabled()) {
            return;
        }
        DebugLogger.log("      << scope EXIT:  {} | identity={}",
                scope, identity != null ? identity : "<none>");
    }

    // --- Equipment slot hiding ---

    public static void equipmentSlotHidingFired(@Nullable String playerName, EquipmentSlot slot, boolean hidden, String reason) {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("    [slotHiding] player={}, slot={}, hidden={}, reason={}", playerName, slot, hidden, reason);
    }
}
