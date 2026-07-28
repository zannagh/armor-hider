package de.zannagh.armorhider.paper.perm;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Produces the vanilla-style permission level the client expects.
 *
 * <p>The client compares {@code PermissionPacket.permissionLevel} numerically against 3, so this
 * must keep emitting integers (0/3/4) rather than a boolean.</p>
 */
public final class PermissionResolver {

    /** The permission that grants server-wide Armor Hider administration. */
    public static final String ADMIN_PERMISSION = "armorhider.admin";

    private static final String LUCKPERMS_CLASS = "net.luckperms.api.LuckPermsProvider";

    private final Logger logger;
    private final boolean luckPermsPresent;
    private boolean luckPermsLogged;

    public PermissionResolver(Logger logger) {
        this.logger = logger;
        this.luckPermsPresent = detectLuckPerms();
    }

    /** Whether LuckPerms was detected at plugin-enable time. */
    public boolean isLuckPermsPresent() {
        return luckPermsPresent;
    }

    /**
     * Resolves the permission level for {@code player}.
     *
     * <p>Operators short-circuit to 4, mirroring the mod's {@code isSingleplayerOwner} / vanilla-op
     * path. Otherwise the Bukkit permission is consulted (which any permissions plugin, LuckPerms
     * included, answers through superperms), and finally the LuckPerms API directly.</p>
     */
    public int getPermissionLevel(Player player) {
        if (player == null) {
            return 0;
        }
        if (player.isOp()) {
            return 4;
        }
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return 4;
        }
        if (!luckPermsPresent) {
            return 0;
        }
        logLuckPermsOnce();
        return LuckPermsHook.getPermissionLevel(player.getUniqueId(), logger);
    }

    private void logLuckPermsOnce() {
        if (luckPermsLogged) {
            return;
        }
        luckPermsLogged = true;
        logger.info("LuckPerms detected - using it for permission checks instead of default permission handling.");
        logger.info("Note: Add permission to users with the following key to let them change armor hider "
                + "settings server-wide: " + LuckPermsHook.ADMIN_PERMISSION);
    }

    private static boolean detectLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return false;
        }
        try {
            Class.forName(LUCKPERMS_CLASS);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
