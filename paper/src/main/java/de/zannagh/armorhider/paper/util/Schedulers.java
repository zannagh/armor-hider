package de.zannagh.armorhider.paper.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Runs off-main-thread work on whichever scheduler the running server actually has.
 *
 * <p>Folia replaced the single main thread with regionised ones, so {@code BukkitScheduler} is not
 * usable there. Folia's schedulers in turn do not exist on Spigot/Bukkit 1.20.1, and this jar is
 * compiled against an old API on purpose - so the Folia path is reached reflectively, keeping the
 * class loadable everywhere.</p>
 */
public final class Schedulers {

    private static final String FOLIA_MARKER = "io.papermc.paper.threadedregions.RegionizedServer";

    private final Plugin plugin;
    private final boolean folia;
    private final Method asyncSchedulerGetter;
    private final Method runNow;

    public Schedulers(Plugin plugin) {
        this.plugin = plugin;
        this.folia = classPresent(FOLIA_MARKER);

        Method getter = null;
        Method run = null;
        if (folia) {
            try {
                getter = Bukkit.class.getMethod("getAsyncScheduler");
                run = getter.getReturnType().getMethod("runNow", Plugin.class, Consumer.class);
            } catch (NoSuchMethodException | LinkageError e) {
                getter = null;
                run = null;
            }
        }
        this.asyncSchedulerGetter = getter;
        this.runNow = run;
    }

    /** Whether the server is Folia (or a Folia derivative). */
    public boolean isFolia() {
        return folia;
    }

    /**
     * Runs {@code task} off the main thread. Used for disk writes; message sends are done directly
     * on the calling thread, which is safe because they only touch the player's netty pipeline.
     */
    public void runAsync(Runnable task) {
        if (asyncSchedulerGetter != null && runNow != null) {
            try {
                Object scheduler = asyncSchedulerGetter.invoke(null);
                Consumer<Object> wrapped = ignored -> task.run();
                runNow.invoke(scheduler, plugin, wrapped);
                return;
            } catch (ReflectiveOperationException | LinkageError e) {
                plugin.getLogger().log(Level.WARNING,
                        "Folia async scheduler unavailable - running the task inline", e);
                task.run();
                return;
            }
        }
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } catch (RuntimeException e) {
            // IllegalPluginAccessException during shutdown, for one - the work still has to complete.
            task.run();
        }
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
