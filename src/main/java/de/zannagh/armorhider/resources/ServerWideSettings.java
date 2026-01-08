package de.zannagh.armorhider.resources;

import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.configuration.items.implementations.CombatDetection;
import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class ServerWideSettings implements ConfigurationSource<ServerWideSettings> {
    public static final Identifier PACKET_ID = new Identifier("de.zannagh.armorhider", "server_wide_settings");

    private boolean hasChangedFromSerializedContent = false;

    @SerializedName(value = "enableCombatDetection")
    public CombatDetection enableCombatDetection;

    public ServerWideSettings() {
        this.enableCombatDetection = new CombatDetection();
    }

    public ServerWideSettings(Boolean enableCombatDetection) {
        this.enableCombatDetection = new CombatDetection(enableCombatDetection);
    }

    @Override
    public Identifier getPacketId() {
        return PACKET_ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        CompressedJsonCodec.encode(this, buf);
    }

    public static ServerWideSettings read(PacketByteBuf buf) {
        return CompressedJsonCodec.decode(buf, ServerWideSettings.class);
    }

    @Override
    public boolean hasChangedFromSerializedContent() {
        return hasChangedFromSerializedContent;
    }

    @Override
    public void setHasChangedFromSerializedContent() {
        hasChangedFromSerializedContent = true;
    }
}
