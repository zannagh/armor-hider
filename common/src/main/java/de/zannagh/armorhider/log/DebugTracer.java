package de.zannagh.armorhider.log;

import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

/**
 * Structured trace-point methods for the rendering pipeline.
 * Each method is a thin wrapper around {@link DebugLogger} that formats
 * a human-readable message for a specific pipeline event.
 * <p>
 * To avoid log flood at 60+ FPS, scope enter/exit for render frames and
 * level renders are sampled: detailed per-entity/per-item messages are
 * only logged every {@value #LOG_EVERY_N_FRAMES}th frame.
 */
public final class DebugTracer {

    private static final int LOG_EVERY_N_FRAMES = 60;
    private static long frameCounter = 0;

    private DebugTracer() {}

    private static boolean shouldLogThisFrame() {
        return frameCounter % LOG_EVERY_N_FRAMES == 0;
    }

    public static void scopeEnterRenderFrame() {
        if (!DebugLogger.isEnabled()) {
            return;
        }
        frameCounter++;
        if (shouldLogThisFrame()) {
            DebugLogger.log(">> enterRenderFrame (frame #{})", frameCounter);
        }
    }

    public static void scopeExitRenderFrame() {
        if (!DebugLogger.isEnabled()) {
            return;
        }
        if (shouldLogThisFrame()) {
            DebugLogger.log("<< exitRenderFrame (frame #{})", frameCounter);
        }
    }

    public static void scopeEnterLevelRender() {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("  >> enterLevelRender");
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

    public static void scopeEnrichEntity(@Nullable String playerName, boolean isPlayer) {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("    ~~ enrichEntityScope: playerName={}, isPlayer={}", playerName, isPlayer);
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

    // --- Identity resolution ---

    public static void identityHintSet(@Nullable String playerName) {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("    [identity] hint SET: {}", playerName);
    }

    public static void identityHintCleared() {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("    [identity] hint CLEARED");
    }

    public static void identityResolved(String source, @Nullable String playerName, boolean isPlayer) {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("    [identity] resolved via {}: playerName={}, isPlayer={}", source, playerName, isPlayer);
    }

    // --- Render decisions ---

    public static void renderDecisionShouldModify(EquipmentSlot slot, @Nullable String playerName, boolean result, String reason) {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("      [decision] shouldModifyEquipment: slot={}, player={}, result={}, reason={}",
                slot, playerName, result, reason);
    }

    public static void renderDecisionShouldCancel(EquipmentSlot slot, @Nullable String playerName, boolean result) {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("      [decision] shouldCancelRender: slot={}, player={}, result={}", slot, playerName, result);
    }

    // --- Equipment slot hiding ---

    public static void equipmentSlotHidingFired(@Nullable String playerName, EquipmentSlot slot, boolean hidden, String reason) {
        if (!DebugLogger.isEnabled() || !shouldLogThisFrame()) {
            return;
        }
        DebugLogger.log("    [slotHiding] player={}, slot={}, hidden={}, reason={}", playerName, slot, hidden, reason);
    }
}
