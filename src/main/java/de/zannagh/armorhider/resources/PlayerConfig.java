
package de.zannagh.armorhider.resources;

import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.configuration.items.implementations.*;
import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.io.Reader;
import java.util.UUID;

public class PlayerConfig implements ConfigurationSource<PlayerConfig> {
    
    private boolean hasChangedFromSerializedContent;

    public static final Id<PlayerConfig> PACKET_IDENTIFIER = new Id<>(Identifier.of("de.zannagh.armorhider", "settings_c2s_packet"));
    
    public PacketCodec<ByteBuf, PlayerConfig> getCodec() {
        return CompressedJsonCodec.create(PlayerConfig.class);
    }

    @Override
    public Id<PlayerConfig> getId() {
        return PACKET_IDENTIFIER;
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
    public static @NonNull PlayerConfig empty() {
        return new PlayerConfig();
    }
    
    @Contract("_, _ -> new")
    public static @NonNull PlayerConfig defaults(UUID playerId, String playerName) {
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