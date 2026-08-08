package de.zannagh.armorhider;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dev-run safety net: halts this game JVM shortly after the Gradle (or IDE) launcher that spawned
 * it dies, so an interrupted {@code runClient}/{@code runServer} - a Ctrl-C, an IDE "Stop", a killed
 * smoke fork - never leaves a multi-GB Minecraft JVM orphaned. Loom/moddev launch the game as a
 * child JVM; when the launcher is hard-killed the child is reparented to init and lingers, which is
 * how "stray javas" accumulate after light testing. This watches the parent process and, once it
 * exits, hard-{@link Runtime#halt(int) halts} rather than orphaning.
 * <p>
 * Armed only when JVM property {@code armorhider.devRun.watchdog=true} (set by the loom/moddev run
 * configs). A no-op in production where the property is absent, and self-disables when the parent
 * process is not visible (e.g. already reparented, or a platform that hides it).
 */
public final class DevRunWatchdog {

    private static final AtomicBoolean ARMED = new AtomicBoolean(false);
    private static final long POLL_INTERVAL_MS = 2_000L;

    private DevRunWatchdog() {}

    public static void maybeArm() {
        if (!Boolean.getBoolean("armorhider.devRun.watchdog")) {
            return;
        }
        if (!ARMED.compareAndSet(false, true)) {
            return;
        }

        Optional<ProcessHandle> parent = ProcessHandle.current().parent();
        if (parent.isEmpty()) {
            ArmorHider.LOGGER.info("[devwatchdog] No parent process visible; watchdog not armed");
            return;
        }

        ProcessHandle launcher = parent.get();
        Thread watchdog = new Thread(() -> watch(launcher), "armorhider-devrun-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        ArmorHider.LOGGER.info("[devwatchdog] Watching launcher pid {}; will halt this game JVM if it dies",
                launcher.pid());
    }

    private static void watch(ProcessHandle launcher) {
        while (launcher.isAlive()) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        // The handle tracks this specific process instance, so isAlive() flips to false only when the
        // launcher we recorded actually terminated (not merely PID reuse). Halt rather than exit: a
        // normal shutdown would hang on the same GLFW/native teardown SmokeMode also has to halt past.
        ArmorHider.LOGGER.warn("[devwatchdog] Launcher pid {} exited; halting orphaned game JVM", launcher.pid());
        Runtime.getRuntime().halt(0);
    }
}
