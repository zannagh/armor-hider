package de.zannagh.armorhider.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

public final class ServerUtil {
    public static int getPermissionLevelForPlayer(Player profile, MinecraftServer server) {
        //? if >= 1.21.11
        return server.getProfilePermissions(profile.nameAndId()).level().id();
        //? if >= 1.21.9 && < 1.21.11
        /*return server.getProfilePermissions(profile.nameAndId());*/
        //? if < 1.21.9
        //return server.getProfilePermissions(profile.getGameProfile());
    }
}
