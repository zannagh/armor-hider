package de.zannagh.armorhider.net.packets;

import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.*;
import de.zannagh.armorhider.configuration.items.*;
import com.google.gson.annotations.Expose;
//? if >= 1.20.5 {
import de.zannagh.armorhider.net.CompressedJsonCodec;
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
/*import net.minecraft.resources.Identifier;
*///?}

public class PlayerConfig implements ConfigurationSource<PlayerConfig> {

    /**
     * Config schema version. Absent (0) in configs from before versioning was introduced.
     * Incremented when the config structure changes in a way that requires migration.
     * <ul>
     *   <li>0 = pre-versioning format (before 0.10.0-pre.5)</li>
     *   <li>1 = introduced explicit configVersion/schema versioning (0.10.0-pre.5+)</li>
     * </ul>
     */
    @SerializedName(value = "configVersion")
    public int configVersion;

    /** The current config schema version. */
    public static final int CURRENT_CONFIG_VERSION = 1;
    
    //? if >= 1.21.11 {
    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "settings_c2s_packet");
    //?}

    //? if >= 1.20.5 && < 1.21.11 {
    /*public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("armorhider", "settings_c2s_packet");
    *///?}

    //? if >= 1.20.5 {
    public static final StreamCodec<ByteBuf, PlayerConfig> STREAM_CODEC = CompressedJsonCodec.create(PlayerConfig.class);

    public static final Type<PlayerConfig> TYPE = new Type<>(PACKET_IDENTIFIER);

    public StreamCodec<ByteBuf, PlayerConfig> getCodec() {
        return CompressedJsonCodec.create(PlayerConfig.class);
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    @SerializedName(value = "helmetOpacity", alternate = {"helmetTransparency"})
    public ArmorOpacity helmetOpacity;
    @SerializedName(value = "helmetGlint")
    public EnableGlint helmetGlint;
    @SerializedName(value = "chestOpacity", alternate = {"chestTransparency"})
    public ArmorOpacity chestOpacity;
    @SerializedName(value = "chestGlint")
    public EnableGlint chestGlint;
    @SerializedName(value = "legsOpacity", alternate = {"legsTransparency"})
    public ArmorOpacity legsOpacity;
    @SerializedName(value = "legsGlint")
    public EnableGlint legsGlint;
    @SerializedName(value = "bootsOpacity", alternate = {"bootsTransparency"})
    public ArmorOpacity bootsOpacity;
    @SerializedName(value = "bootsGlint")
    public EnableGlint bootsGlint;
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
    @SerializedName(value = "offHandOpacity")
    public OffHandOpacity offHandOpacity;
    
    public PlayerUuid playerId;

    /** The name of the player, derived from the display name. */
    public PlayerName playerName;

    /** Per-item exclusion configuration. Null-safe via {@link #getExclusionItems()}. */
    @SerializedName(value = "exclusionItems")
    public ExclusionItemConfiguration exclusionItems;

    private transient boolean hasChangedFromSerializedContent;
    
    public PlayerConfig(UUID uuid, String name) {
        this();
        this.playerId = new PlayerUuid(uuid);
        this.playerName = new PlayerName(name);
        this.configVersion = CURRENT_CONFIG_VERSION;
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
        offHandOpacity = new OffHandOpacity();
        helmetGlint = new EnableGlint();
        chestGlint = new EnableGlint();
        legsGlint = new EnableGlint();
        bootsGlint = new EnableGlint();
        exclusionItems = ExclusionItemConfiguration.defaults();
    }

    /**
     * Returns the exclusion item configuration, initializing with defaults if null
     * (for backwards compatibility with configs saved before this field existed).
     */
    public ExclusionItemConfiguration getExclusionItems() {
        if (exclusionItems == null) {
            exclusionItems = ExclusionItemConfiguration.defaults();
        }
        return exclusionItems;
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

    /**
     * Migrates a config from an older schema version to the current one.
     * Creates a fresh config with all current defaults, then overlays
     * the values that were present in the old config.
     */
    public static @NonNull PlayerConfig migrate(@NonNull PlayerConfig old) {
        ArmorHider.LOGGER.info("Migrating player config for {} from version {} to {}.",
                old.playerName.getValue(), old.configVersion, CURRENT_CONFIG_VERSION);

        var fresh = new PlayerConfig(old.playerId.getValue(), old.playerName.getValue());

        fresh.helmetOpacity.setValue(old.helmetOpacity.getValue());
        fresh.chestOpacity.setValue(old.chestOpacity.getValue());
        fresh.legsOpacity.setValue(old.legsOpacity.getValue());
        fresh.bootsOpacity.setValue(old.bootsOpacity.getValue());
        fresh.helmetGlint.setValue(old.helmetGlint.getValue());
        fresh.chestGlint.setValue(old.chestGlint.getValue());
        fresh.legsGlint.setValue(old.legsGlint.getValue());
        fresh.bootsGlint.setValue(old.bootsGlint.getValue());
        fresh.enableCombatDetection.setValue(old.enableCombatDetection.getValue());
        fresh.opacityAffectingElytra.setValue(old.opacityAffectingElytra.getValue());
        fresh.opacityAffectingHatOrSkull.setValue(old.opacityAffectingHatOrSkull.getValue());
        fresh.disableArmorHider.setValue(old.disableArmorHider.getValue());
        fresh.disableArmorHiderForOthers.setValue(old.disableArmorHiderForOthers.getValue());
        fresh.usePlayerSettingsWhenUndeterminable.setValue(old.usePlayerSettingsWhenUndeterminable.getValue());
        fresh.offHandOpacity.setValue(old.offHandOpacity.getValue());

        if (old.exclusionItems != null) {
            fresh.exclusionItems = old.exclusionItems.deepCopy();
        }

        fresh.setHasChangedFromSerializedContent();
        return fresh;
    }

    @Override
    public boolean hasChangedFromSerializedContent() {
        return hasChangedFromSerializedContent;
    }

    @Override
    public void setHasChangedFromSerializedContent() {
        hasChangedFromSerializedContent = true;
    }
    
    public String toJson() {
        return ArmorHider.GSON.toJson(this);   
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
        newConfig.offHandOpacity.setValue(this.offHandOpacity.getValue());
        newConfig.helmetGlint.setValue(this.helmetGlint.getValue());
        newConfig.chestGlint.setValue(this.chestGlint.getValue());
        newConfig.legsGlint.setValue(this.legsGlint.getValue());
        newConfig.bootsGlint.setValue(this.bootsGlint.getValue());
        newConfig.exclusionItems = this.getExclusionItems().deepCopy();
        return newConfig;
    }
}
