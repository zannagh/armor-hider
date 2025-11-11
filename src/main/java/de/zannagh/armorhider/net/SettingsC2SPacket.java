package de.zannagh.armorhider.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.zannagh.armorhider.PlayerConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SettingsC2SPacket(PlayerConfig config) implements CustomPayload {

    public PlayerConfig payload(){
        return config;
    }
    private static final Gson GSON = new GsonBuilder().create();
    public static final CustomPayload.Id<SettingsC2SPacket> IDENTIFIER = new CustomPayload.Id<>(Identifier.of("de.zannagh.armorhider", "settings_c2s_packet"));
    public static final PacketCodec<ByteBuf, SettingsC2SPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, c -> c.config().helmetTransparency,
            PacketCodecs.DOUBLE, c -> c.config().chestTransparency,
            PacketCodecs.DOUBLE, c -> c.config().legsTransparency,
            PacketCodecs.DOUBLE, c -> c.config().bootsTransparency,
            PacketCodecs.STRING, c -> c.config().playerId.toString(),
            (helmet, chest, legs, boots, uuid) -> new SettingsC2SPacket(PlayerConfig.FromPacket(helmet, chest, legs, boots, uuid))
    );
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return IDENTIFIER;
    }
}
