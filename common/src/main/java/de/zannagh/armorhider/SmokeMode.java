package de.zannagh.armorhider;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Smoke-test mode: arms a one-shot timer that exits the JVM cleanly some seconds after
 * {@link #maybeArm()} is called. Used by CI and the {@code scripts/smoke-all} runners
 * to verify "MC + this mod (+ optionally compat mods) boot without crashing".
 * <p>
 * Activated by JVM property {@code armorhider.smoke.exit=true}. Optional knobs:
 * <ul>
 *   <li>{@code armorhider.smoke.delay.ms} — milliseconds to wait after arming before exit.
 *       Default {@code 15000}. Set higher on cold CI runs where the initial resource
 *       reload takes longer.</li>
 *   <li>{@code armorhider.smoke.halt.timeout.ms} — fallback hard-halt delay if
 *       {@code System.exit} hangs on LWJGL/GLFW native shutdown. Default {@code 5000}.</li>
 * </ul>
 * No-op when the property is unset, so dev runs are unaffected.
 */
public final class SmokeMode {

    private static final AtomicBoolean ARMED = new AtomicBoolean(false);

    private SmokeMode() {}

    public static void maybeArm() {
        if (!Boolean.getBoolean("armorhider.smoke.exit")) return;
        if (!ARMED.compareAndSet(false, true)) return;

        long delayMs = Long.getLong("armorhider.smoke.delay.ms", 15_000L);
        long haltMs = Long.getLong("armorhider.smoke.halt.timeout.ms", 5_000L);

        Thread timer = new Thread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            ArmorHider.LOGGER.info("[smoke] Boot smoke window of {}ms elapsed cleanly, exiting 0", delayMs);

            Thread haltFallback = new Thread(() -> {
                try {
                    Thread.sleep(haltMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
                ArmorHider.LOGGER.warn("[smoke] System.exit did not return within {}ms, halting", haltMs);
                Runtime.getRuntime().halt(0);
            }, "armorhider-smoke-halt-fallback");
            haltFallback.setDaemon(true);
            haltFallback.start();

            System.exit(0);
        }, "armorhider-smoke-exit-timer");
        timer.setDaemon(true);
        timer.start();

        ArmorHider.LOGGER.info("[smoke] Armed: will exit JVM in {}ms unless a crash trips us first", delayMs);
    }
}
