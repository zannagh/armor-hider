package de.zannagh.armorhider.netPackets;

import de.zannagh.armorhider.resources.PlayerConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.ArrayList;

public record SettingsS2CPacket(List<PlayerConfig> config) implements CustomPayload {
    public static final Id<SettingsS2CPacket> IDENTIFIER = new Id<>(Identifier.of("de.zannagh.armorhider", "settings_s2c_packet"));

    private static final PacketCodec<ByteBuf, PlayerConfig> PLAYER_CONFIG_CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, pc -> pc.helmetTransparency,
            PacketCodecs.DOUBLE, pc -> pc.chestTransparency,
            PacketCodecs.DOUBLE, pc -> pc.legsTransparency,
            PacketCodecs.DOUBLE, pc -> pc.bootsTransparency,
            PacketCodecs.BOOLEAN, pc -> pc.enableCombatDetection,
            PacketCodecs.STRING, pc -> pc.playerId.toString(),
            PacketCodecs.STRING, pc -> pc.playerName,
            PlayerConfig::FromPacket
    );

    public static final PacketCodec<ByteBuf, SettingsS2CPacket> PACKET_CODEC = PacketCodecs.collection(
            ArrayList::new, PLAYER_CONFIG_CODEC
    ).xmap(SettingsS2CPacket::new, packet -> new ArrayList<>(packet.config()));
    @Override
    public Id<? extends CustomPayload> getId() {
        return IDENTIFIER;
    }
}
