package de.zannagh.armorhider.paper.perm;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Isolated hook into the LuckPerms API.
 *
 * <p>This class must ONLY be loaded when LuckPerms is confirmed present on the classpath, otherwise
 * it throws {@link NoClassDefFoundError}. {@link PermissionResolver} guards the entry point.</p>
 */
public final class LuckPermsHook {

    /** The node that grants server-wide Armor Hider administration. */
    public static final String ADMIN_PERMISSION = "armorhider.admin";

    private LuckPermsHook() {
    }

    /**
     * Maps the {@code armorhider.admin} LuckPerms node to a vanilla-style permission level.
     *
     * @return 4 if the player holds the node, 0 otherwise
     */
    public static int getPermissionLevel(UUID playerUuid, Logger logger) {
        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(playerUuid);
            if (user == null) {
                return 0;
            }
            CachedPermissionData permissionData = user.getCachedData().getPermissionData();
            if (permissionData.checkPermission(ADMIN_PERMISSION).asBoolean()) {
                return 4;
            }
            return 0;
        } catch (Exception | LinkageError e) {
            logger.log(Level.WARNING, "Failed to query LuckPerms for player " + playerUuid, e);
            return 0;
        }
    }
}
