package de.zannagh.armorhider.resources;

import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.configuration.items.implementations.CombatDetection;
import de.zannagh.armorhider.configuration.items.implementations.ForceArmorHiderOffOnPlayers;
import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

public class ServerWideSettings implements ConfigurationSource<ServerWideSettings> {
    public static final Id<ServerWideSettings> PACKET_IDENTIFIER = new Id<>(Identifier.of("de.zannagh.armorhider", "server_wide_settings"));

    private transient boolean hasChangedFromSerializedContent = false;

    @SerializedName(value = "enableCombatDetection")
    public CombatDetection enableCombatDetection;
    
    @SerializedName(value = "forceArmorHiderOff")
    public ForceArmorHiderOffOnPlayers forceArmorHiderOff;

    public ServerWideSettings() {
        this.enableCombatDetection = new CombatDetection();
        this.forceArmorHiderOff = new ForceArmorHiderOffOnPlayers();
    }

    public ServerWideSettings(Boolean enableCombatDetection, Boolean forceArmorHiderOff) {
        this.enableCombatDetection = new CombatDetection(enableCombatDetection);
        this.forceArmorHiderOff = new ForceArmorHiderOffOnPlayers(forceArmorHiderOff);
    }

    @Override
    public Id<ServerWideSettings> getId() {
        return PACKET_IDENTIFIER;
    }

    @Override
    public PacketCodec<ByteBuf, ServerWideSettings> getCodec() {
        return CompressedJsonCodec.create(ServerWideSettings.class);
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
