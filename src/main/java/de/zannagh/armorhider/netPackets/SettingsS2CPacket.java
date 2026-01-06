package de.zannagh.armorhider.netPackets;

import de.zannagh.armorhider.resources.PlayerConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.ArrayList;

public record SettingsS2CPacket(List<PlayerConfig> config, Boolean serverCombatDetection) implements CustomPayload {
    public static final Id<SettingsS2CPacket> IDENTIFIER = new Id<>(Identifier.of("de.zannagh.armorhider", "settings_s2c_packet"));

    private static final PacketCodec<ByteBuf, PlayerConfig> PLAYER_CONFIG_CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, pc -> pc.helmetOpacity.getValue(),
            PacketCodecs.DOUBLE, pc -> pc.chestOpacity.getValue(),
            PacketCodecs.DOUBLE, pc -> pc.legsOpacity.getValue(),
            PacketCodecs.DOUBLE, pc -> pc.bootsOpacity.getValue(),
            PacketCodecs.BOOLEAN, pc -> pc.enableCombatDetection.getValue(),
            PacketCodecs.STRING, pc -> pc.playerId.getValue().toString(),
            PacketCodecs.STRING, pc -> pc.playerName.getValue(),
            PlayerConfig::new
    );

    public static final PacketCodec<ByteBuf, SettingsS2CPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.collection(ArrayList::new, PLAYER_CONFIG_CODEC), SettingsS2CPacket::config,
            PacketCodecs.BOOLEAN.cast(), packet -> packet.serverCombatDetection,
            SettingsS2CPacket::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return IDENTIFIER;
    }
}
