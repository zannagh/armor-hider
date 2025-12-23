package de.zannagh.armorhider.netPackets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.zannagh.armorhider.resources.PlayerConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SettingsC2SPacket(PlayerConfig config) implements CustomPayload {
    public static final Id<SettingsC2SPacket> IDENTIFIER = new Id<>(Identifier.of("de.zannagh.armorhider", "settings_c2s_packet"));
    public static final PacketCodec<ByteBuf, SettingsC2SPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, c -> c.config().helmetTransparency,
            PacketCodecs.DOUBLE, c -> c.config().chestTransparency,
            PacketCodecs.DOUBLE, c -> c.config().legsTransparency,
            PacketCodecs.DOUBLE, c -> c.config().bootsTransparency,
            PacketCodecs.STRING, c -> c.config().playerId.toString(),
            PacketCodecs.STRING, c -> c.config().playerName,
            (helmet, chest, legs, boots, uuid, playerName) -> new SettingsC2SPacket(PlayerConfig.FromPacket(helmet, chest, legs, boots, uuid, playerName))
    );
    @Override
    public Id<? extends CustomPayload> getId() {
        return IDENTIFIER;
    }
}
