package de.zannagh.armorhider.netPackets;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record AdminSettingsC2SPacket(boolean enableCombatDetection) implements CustomPacketPayload {
    public static final Identifier IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "admin_settings_c2s_packet");
    public static final StreamCodec<ByteBuf, AdminSettingsC2SPacket> PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, c -> c.enableCombatDetection,
            AdminSettingsC2SPacket::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return new Type<>(IDENTIFIER);
    }
}
