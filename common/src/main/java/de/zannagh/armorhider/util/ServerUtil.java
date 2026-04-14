package de.zannagh.armorhider.util;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.server.LuckPermsHook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

public final class ServerUtil {

    private static final boolean LUCKPERMS_AVAILABLE = classExists("net.luckperms.api.LuckPermsProvider");

    static {
        if (LUCKPERMS_AVAILABLE) {
            ArmorHider.LOGGER.info("LuckPerms detected — using it for permission checks instead of default permission handling.");
            ArmorHider.LOGGER.info("--- Armor Hider Configuration ---");
            ArmorHider.LOGGER.info("Add permission to users with the following key to let them change armor hider settings server-wide: {}", LuckPermsHook.ADMIN_PERMISSION);
            ArmorHider.LOGGER.info("--- End of Armor Hider Configuration ---");
        }
    }

    public static int getPermissionLevelForPlayer(Player player, MinecraftServer server) {
        if (LUCKPERMS_AVAILABLE) {
            if (getVanillaPermissionLevel(player, server) >= 4) {
                return 4;
            }
            return LuckPermsHook.getPermissionLevel(player.getUUID());
        }
        return getVanillaPermissionLevel(player, server);
    }

    private static int getVanillaPermissionLevel(Player player, MinecraftServer server) {
        //? if >= 1.21.11
        return server.getProfilePermissions(player.nameAndId()).level().id();
        //? if >= 1.21.9 && < 1.21.11
        //return server.getProfilePermissions(player.nameAndId());
        //? if < 1.21.9
        //return server.getProfilePermissions(player.getGameProfile());
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, ServerUtil.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
