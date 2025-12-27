package de.zannagh.armorhider.netPackets;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AdminSettingsC2SPacket(boolean enableCombatDetection) implements CustomPayload {
    public static final Id<AdminSettingsC2SPacket> IDENTIFIER = new Id<>(Identifier.of("de.zannagh.armorhider", "admin_settings_c2s_packet"));
    public static final PacketCodec<ByteBuf, AdminSettingsC2SPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, c -> c.enableCombatDetection,
            AdminSettingsC2SPacket::new
    );
    @Override
    public Id<? extends CustomPayload> getId() {
        return IDENTIFIER;
    }
}
