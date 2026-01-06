package de.zannagh.armorhider.netPackets;

import de.zannagh.armorhider.resources.PlayerConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SettingsC2SPacket(PlayerConfig config) implements CustomPayload {
    public static final Id<SettingsC2SPacket> IDENTIFIER = new Id<>(Identifier.of("de.zannagh.armorhider", "settings_c2s_packet"));
    public static final PacketCodec<ByteBuf, SettingsC2SPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, c -> c.config().helmetOpacity.getValue(),
            PacketCodecs.DOUBLE, c -> c.config().chestOpacity.getValue(),
            PacketCodecs.DOUBLE, c -> c.config().legsOpacity.getValue(),
            PacketCodecs.DOUBLE, c -> c.config().bootsOpacity.getValue(),
            PacketCodecs.BOOLEAN, c -> c.config().enableCombatDetection.getValue(),
            PacketCodecs.STRING, c -> c.config().playerId.getValue().toString(),
            PacketCodecs.STRING, c -> c.config().playerName.getValue(),
            (helmet, chest, legs, boots, combatDetection, uuid, playerName) -> new SettingsC2SPacket(new PlayerConfig(helmet, chest, legs, boots, combatDetection != null ? combatDetection : true, uuid, playerName))
    );
    @Override
    public Id<? extends CustomPayload> getId() {
        return IDENTIFIER;
    }
}
