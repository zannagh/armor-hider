//? if >= 1.20.5 {
package de.zannagh.armorhider.netPackets;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;
//? if >= 1.21.11 {
import net.minecraft.resources.Identifier;
//?}
//? if >= 1.20.5 && < 1.21.11 {
/*import net.minecraft.resources.ResourceLocation;
*///?}

public class PermissionPacket implements CustomPacketPayload {

    //? if >= 1.21.11 {
    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "permissions_s2c_packet");
    //?}
    //? if >= 1.20.5 && < 1.21.11 {
    /*public static final ResourceLocation PACKET_IDENTIFIER = ResourceLocation.fromNamespaceAndPath("armorhider", "permissions_s2c_packet");
    *///?}
    public static final StreamCodec<ByteBuf, PermissionPacket> STREAM_CODEC = CompressedJsonCodec.create(PermissionPacket.class);

    public static final Type<PermissionPacket> TYPE = new Type<>(PACKET_IDENTIFIER);

    public int permissionLevel;

    public PermissionPacket(Player player, MinecraftServer server) {
        //? if >= 1.21.11 {
        this.permissionLevel = server.getProfilePermissions(player.nameAndId()).level().id();
         //?}
        //? if >= 1.21.9 && < 1.21.11 {
        /*this.permissionLevel = server.getProfilePermissions(player.nameAndId());
        *///?}
        //? if >= 1.20.5 && < 1.21.9 {
        /*this.permissionLevel = server.getProfilePermissions(player.getGameProfile());
        *///?}
    }

    public PermissionPacket(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public PermissionPacket() {
        permissionLevel = 0;
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//?}

//? if < 1.20.5 {
/*package de.zannagh.armorhider.netPackets;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

// Simplified PermissionPacket for 1.20.x - no CustomPacketPayload interface
public class PermissionPacket {

    public int permissionLevel;

    public PermissionPacket(Player player, MinecraftServer server) {
        this.permissionLevel = server.getProfilePermissions(player.getGameProfile());
    }

    public PermissionPacket(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public PermissionPacket() {
        permissionLevel = 0;
    }
}
*///?}
