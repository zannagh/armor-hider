package de.zannagh.armorhider.netPackets;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

public class PermissionPacket implements CustomPacketPayload {

    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "permissions_s2c_packet");
    public static final StreamCodec<ByteBuf, PermissionPacket> STREAM_CODEC = CompressedJsonCodec.create(PermissionPacket.class);
    public static final Type<PermissionPacket> TYPE = new Type<>(PACKET_IDENTIFIER);

    public int permissionLevel;

    public PermissionPacket(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
    
    public PermissionPacket(Player player, MinecraftServer server) {
        this.permissionLevel = server.getProfilePermissions(player.nameAndId()).level().id();
    }
    
    public PermissionPacket(){
        permissionLevel = 0;
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
