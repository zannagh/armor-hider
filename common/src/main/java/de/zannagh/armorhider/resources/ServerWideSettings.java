package de.zannagh.armorhider.resources;

import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.common.configuration.items.implementations.CombatDetection;
import de.zannagh.armorhider.common.configuration.items.implementations.ForceArmorHiderOffOnPlayers;
//? if >= 1.20.5 {
import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import org.jspecify.annotations.NonNull;

//? if >= 1.21.11 {
import net.minecraft.resources.Identifier;
 //?}
//? if >= 1.20.5 && < 1.21.11 {
/*import net.minecraft.resources.ResourceLocation;
*///?}

public class ServerWideSettings implements ConfigurationSource<ServerWideSettings> {

    //? if >= 1.21.11 {
    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "server_wide_settings");
     //?}
    //? if >= 1.20.5 && < 1.21.11 {
    /*public static final ResourceLocation PACKET_IDENTIFIER = ResourceLocation.fromNamespaceAndPath("armorhider", "server_wide_settings");
    *///?}

    //? if >= 1.20.5 {
    public static final Type<ServerWideSettings> TYPE = new Type<>(PACKET_IDENTIFIER);

    public static final StreamCodec<ByteBuf, ServerWideSettings> STREAM_CODEC = CompressedJsonCodec.create(ServerWideSettings.class);

    @Override
    public StreamCodec<ByteBuf, ServerWideSettings> getCodec() {
        return CompressedJsonCodec.create(ServerWideSettings.class);
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    @SerializedName(value = "enableCombatDetection")
    public CombatDetection enableCombatDetection;
    @SerializedName(value = "forceArmorHiderOff")
    public ForceArmorHiderOffOnPlayers forceArmorHiderOff;
    private transient boolean hasChangedFromSerializedContent = false;

    public ServerWideSettings() {
        this.enableCombatDetection = new CombatDetection();
        this.forceArmorHiderOff = new ForceArmorHiderOffOnPlayers();
    }

    public ServerWideSettings(Boolean enableCombatDetection, Boolean forceArmorHiderOff) {
        this.enableCombatDetection = new CombatDetection(enableCombatDetection);
        this.forceArmorHiderOff = new ForceArmorHiderOffOnPlayers(forceArmorHiderOff);
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
