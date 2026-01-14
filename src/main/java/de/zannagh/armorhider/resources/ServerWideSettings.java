package de.zannagh.armorhider.resources;

import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.configuration.items.implementations.CombatDetection;
import de.zannagh.armorhider.configuration.items.implementations.ForceArmorHiderOffOnPlayers;
import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class ServerWideSettings implements ConfigurationSource<ServerWideSettings> {
    @NotNull public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "server_wide_settings");

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
    public Identifier getId() {
        return PACKET_IDENTIFIER;
    }

    @Override
    public StreamCodec<ByteBuf, ServerWideSettings> getCodec() {
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

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return new Type<>(PACKET_IDENTIFIER);
    }
    
    public static final Type<ServerWideSettings> TYPE = new Type<>(PACKET_IDENTIFIER);
    
    public static final StreamCodec<ByteBuf, ServerWideSettings> STREAM_CODEC = CompressedJsonCodec.create(ServerWideSettings.class);
}
