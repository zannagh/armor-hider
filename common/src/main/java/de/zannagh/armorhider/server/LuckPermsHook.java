package de.zannagh.armorhider.server;

import de.zannagh.armorhider.ArmorHider;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;

import java.util.UUID;

/**
 * Isolated hook into the LuckPerms API.
 * This class must ONLY be loaded when LuckPerms is confirmed present on the classpath,
 * otherwise it will throw {@link NoClassDefFoundError}.
 */
public final class LuckPermsHook {

    public static final String ADMIN_PERMISSION = "armorhider.admin";

    /**
     * Checks whether the given player has the {@code armorhider.admin} permission
     * via LuckPerms and maps the result to a vanilla-style permission level.
     *
     * @return 4 if the player has admin permission, 0 otherwise
     */
    public static int getPermissionLevel(UUID playerUuid) {
        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(playerUuid);
            if (user == null) {
                return 0;
            }

            CachedPermissionData permData = user.getCachedData().getPermissionData();
            if (permData.checkPermission(ADMIN_PERMISSION).asBoolean()) {
                return 4;
            }
            return 0;
        } catch (Exception | LinkageError e) {
            ArmorHider.LOGGER.warn("Failed to query LuckPerms for player {}: {}", playerUuid, e.getMessage());
            return 0;
        }
    }
}
