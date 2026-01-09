
package de.zannagh.armorhider.resources;

import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.configuration.items.implementations.*;
import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.util.Objects;
import java.util.UUID;

public class PlayerConfig implements ConfigurationSource<PlayerConfig> {

    private boolean hasChangedFromSerializedContent;

    @NotNull public static final Identifier PACKET_ID = Objects.requireNonNull(Identifier.of("de.zannagh.armorhider", "settings_c2s_packet"));

    @Override
    public Identifier getPacketId() {
        return PACKET_ID;
    }

    @Override
    public void write(PacketByteBuf buf) {
        CompressedJsonCodec.encode(this, buf);
    }

    public static PlayerConfig read(PacketByteBuf buf) {
        return CompressedJsonCodec.decode(buf, PlayerConfig.class);
    }
    
    @SerializedName(value = "helmetOpacity", alternate = {"helmetTransparency"})
    public ArmorOpacity helmetOpacity;
    
    @SerializedName(value = "chestOpacity", alternate = {"chestTransparency"})
    public ArmorOpacity chestOpacity;

    @SerializedName(value = "legsOpacity", alternate = {"legsTransparency"})
    public ArmorOpacity legsOpacity;

    @SerializedName(value = "bootsOpacity", alternate = {"bootsTransparency"})
    public ArmorOpacity bootsOpacity;
    
    @SerializedName(value = "enableCombatDetection")
    public CombatDetection enableCombatDetection;
    
    @SerializedName(value = "opacityAffectingElytra")
    public OpacityAffectingElytraItem opacityAffectingElytra;
    
    @SerializedName(value = "opacityAffectingHatOrSkull")
    public OpacityAffectingHatOrSkullItem opacityAffectingHatOrSkull;
    
    public PlayerUuid playerId;
    public PlayerName playerName;

    public PlayerConfig(UUID uuid, String name) {
        this();
        this.playerId = new PlayerUuid(uuid);
        this.playerName = new PlayerName(name);
    }
    
    public PlayerConfig() {
        helmetOpacity = new ArmorOpacity();
        chestOpacity = new ArmorOpacity();
        legsOpacity = new ArmorOpacity();
        bootsOpacity = new ArmorOpacity();
        enableCombatDetection = new CombatDetection();
        playerId = new PlayerUuid();
        playerName = new PlayerName();
        opacityAffectingHatOrSkull = new OpacityAffectingHatOrSkullItem();
        opacityAffectingElytra = new OpacityAffectingElytraItem();
    }

    public static PlayerConfig deserialize(Reader reader){
        return ArmorHider.GSON.fromJson(reader, PlayerConfig.class);
    }

    public static PlayerConfig deserialize(String content){
        return ArmorHider.GSON.fromJson(content, PlayerConfig.class);
    }

    @Contract("-> new")
    public static @NotNull PlayerConfig empty() {
        return new PlayerConfig();
    }
    
    @Contract("_, _ -> new")
    public static @NotNull PlayerConfig defaults(UUID playerId, String playerName) {
        return new PlayerConfig(playerId, playerName);
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