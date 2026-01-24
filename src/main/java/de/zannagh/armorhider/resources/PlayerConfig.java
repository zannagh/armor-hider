package de.zannagh.armorhider.resources;

import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.configuration.items.implementations.*;
//? if >= 1.20.5 {
import de.zannagh.armorhider.netPackets.CompressedJsonCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.io.Reader;
import java.util.UUID;

//? if >= 1.21.11 {
import net.minecraft.resources.Identifier;
 //?}
//? if >= 1.20.5 && < 1.21.11 {
/*import net.minecraft.resources.ResourceLocation;
*///?}

public class PlayerConfig implements ConfigurationSource<PlayerConfig> {

    //? if >= 1.21.11 {
    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "settings_c2s_packet");
    //?}

    //? if >= 1.20.5 && < 1.21.11 {
    /*public static final ResourceLocation PACKET_IDENTIFIER = ResourceLocation.fromNamespaceAndPath("armorhider", "settings_c2s_packet");
    *///?}

    //? if >= 1.20.5 {
    public static final StreamCodec<ByteBuf, PlayerConfig> STREAM_CODEC = CompressedJsonCodec.create(PlayerConfig.class);

    public static final Type<PlayerConfig> TYPE = new Type<>(PACKET_IDENTIFIER);
    //?}

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
    @SerializedName(value = "disableArmorHider", alternate = "globalArmorHiderToggle")
    public DisableArmorHiderGlobally disableArmorHider;
    @SerializedName(value = "disableArmorHiderForOthers", alternate = "toggleArmorHiderForOthers")
    public DisableArmorHiderForOthers disableArmorHiderForOthers;
    @SerializedName(value = "usePlayerSettingsWhenUndeterminable")
    public UsePlayerSettingsWhenUndeterminable usePlayerSettingsWhenUndeterminable;
    public PlayerUuid playerId;
    public PlayerName playerName;
    private transient boolean hasChangedFromSerializedContent;
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
        disableArmorHider = new DisableArmorHiderGlobally();
        disableArmorHiderForOthers = new DisableArmorHiderForOthers();
        usePlayerSettingsWhenUndeterminable = new UsePlayerSettingsWhenUndeterminable();
    }

    public static PlayerConfig deserialize(Reader reader) {
        return ArmorHider.GSON.fromJson(reader, PlayerConfig.class);
    }

    public static PlayerConfig deserialize(String content) {
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

    //? if >= 1.20.5 {
    public StreamCodec<ByteBuf, PlayerConfig> getCodec() {
        return CompressedJsonCodec.create(PlayerConfig.class);
    }
    //?}

    @Override
    public boolean hasChangedFromSerializedContent() {
        return hasChangedFromSerializedContent;
    }

    @Override
    public void setHasChangedFromSerializedContent() {
        hasChangedFromSerializedContent = true;
    }

    public PlayerConfig deepCopy(String playerName, UUID playerId) {
        var newConfig = new PlayerConfig(playerId, playerName);
        newConfig.disableArmorHider.setValue(this.disableArmorHider.getValue());
        newConfig.disableArmorHiderForOthers.setValue(this.disableArmorHiderForOthers.getValue());
        newConfig.helmetOpacity.setValue(this.helmetOpacity.getValue());
        newConfig.chestOpacity.setValue(this.chestOpacity.getValue());
        newConfig.legsOpacity.setValue(this.legsOpacity.getValue());
        newConfig.bootsOpacity.setValue(this.bootsOpacity.getValue());
        newConfig.enableCombatDetection.setValue(this.enableCombatDetection.getValue());
        newConfig.opacityAffectingHatOrSkull.setValue(this.opacityAffectingHatOrSkull.getValue());
        newConfig.opacityAffectingElytra.setValue(this.opacityAffectingElytra.getValue());
        newConfig.usePlayerSettingsWhenUndeterminable.setValue(this.usePlayerSettingsWhenUndeterminable.getValue());
        return newConfig;
    }

    //? if >= 1.20.5 {
    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
