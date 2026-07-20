package de.zannagh.armorhider.util;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.server.LuckPermsHook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

public final class ServerUtil {

    private static boolean LUCKPERMS_LOGGED;

    public static int getPermissionLevelForPlayer(Player player, MinecraftServer server) {
        if (CompatManager.requiresCompatTo(CompatFlags.LUCK_PERMS)) {
            if (!LUCKPERMS_LOGGED) {
                LUCKPERMS_LOGGED = true;
                ArmorHider.LOGGER.info("LuckPerms detected — using it for permission checks instead of default permission handling.");
                ArmorHider.LOGGER.info("Note: Add permission to users with the following key to let them change armor hider settings server-wide: {}", LuckPermsHook.ADMIN_PERMISSION);
            }
            if (getVanillaPermissionLevel(player, server) >= 4) {
                return 4;
            }
            return LuckPermsHook.getPermissionLevel(player.getUUID());
        }
        return getVanillaPermissionLevel(player, server);
    }

    private static int getVanillaPermissionLevel(Player player, MinecraftServer server) {
        //? if >= 1.21.9 {
        if (server.isSingleplayerOwner(player.nameAndId())) {
            return 4;
        }
        //?} else {
        /*if (server.isSingleplayerOwner(player.getGameProfile())) {
            return 4;
        }
        *///?}
        //? if >= 1.21.11
        return server.getProfilePermissions(player.nameAndId()).level().id();
        //? if >= 1.21.9 && < 1.21.11
        //return server.getProfilePermissions(player.nameAndId());
        //? if < 1.21.9
        //return server.getProfilePermissions(player.getGameProfile());
    }
}
