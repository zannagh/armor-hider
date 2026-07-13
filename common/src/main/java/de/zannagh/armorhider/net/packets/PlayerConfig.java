package de.zannagh.armorhider.net.packets;

import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.*;
import de.zannagh.armorhider.configuration.items.*;
import de.zannagh.armorhider.configuration.items.InCombatUseDefaultArmorSkin;
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

/**
 * Represents the configuration settings for a player with various customizable options.
 * This class provides serialization, deserialization, deep copy, and migration capabilities.
 * It also supports network-related data transmission and handles configurations in different contexts.<br/><br/>
 *
 * Also see {@link ConfigurationSource}.
 *
 * @since 0.5.0
 */
public class PlayerConfig implements ConfigurationSource<PlayerConfig> {

    @SerializedName(value = "configVersion")
    public int configVersion;

    /** The current config schema version. */
    public static final int CURRENT_CONFIG_VERSION = 8;

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

    /**
     * The opacity that the helmet slot should be rendered at. Also see {@link ArmorOpacity}.<br/><br/>
     *
     * This was part of the initial release and is considered core functionality of Armor Hider.<br/>
     * Note that until 0.10.0-pre.5 the configuration items were not versionized but stored as plain values (schema 1 was introduced in 0.10.0-pre.5).
     *
     * @since AH 0.1.0, schema 1
     */
    @SerializedName(value = "helmetOpacity", alternate = {"helmetTransparency"})
    public @NonNull ArmorOpacity helmetOpacity;

    /**
     * The opacity that the chest slot should be rendered at. Also see {@link ArmorOpacity}.<br/><br/>
     *
     * This was part of the initial release and is considered core functionality of Armor Hider.<br/>
     * Note that until 0.10.0-pre.5 the configuration items were not versionized but stored as plain values (schema 1 was introduced in 0.10.0-pre.5).
     *
     * @since AH 0.1.0, schema 1
     */
    @SerializedName(value = "chestOpacity", alternate = {"chestTransparency"})
    public @NonNull ArmorOpacity chestOpacity;

    /**
     * The opacity that the legs slot should be rendered at. Also see {@link ArmorOpacity}.<br/><br/>
     *
     * This was part of the initial release and is considered core functionality of Armor Hider.<br/>
     * Note that until 0.10.0-pre.5 the configuration items were not versionized but stored as plain values (schema 1 was introduced in 0.10.0-pre.5).
     *
     * @since AH 0.1.0, schema 1
     */
    @SerializedName(value = "legsOpacity", alternate = {"legsTransparency"})
    public @NonNull ArmorOpacity legsOpacity;

    /**
     * The opacity that the boots slot should be rendered at. Also see {@link ArmorOpacity}.<br/><br/>
     *
     * This was part of the initial release and is considered core functionality of Armor Hider.<br/>
     * Note that until 0.10.0-pre.5 the configuration items were not versionized but stored as plain values (schema 1 was introduced in 0.10.0-pre.5).
     *
     * @since AH 0.1.0, schema 1
     */
    @SerializedName(value = "bootsOpacity", alternate = {"bootsTransparency"})
    public @NonNull ArmorOpacity bootsOpacity;

    /**
     * Whether enchantment glint should be drawn on the helmet slot. Also
     * see {@link EnableGlint}.<br/><br/>
     * This was introduced on 8th March 2026 in PR #114.
     *
     * @since AH 0.8.9, schema 1
     */
    @SerializedName(value = "helmetGlint")
    public @NonNull EnableGlint helmetGlint;

    /**
     * Whether enchantment glint should be drawn on the chest slot. Also
     * see {@link EnableGlint}.<br/><br/>
     * This was introduced on 8th March 2026 in PR #114.
     *
     * @since AH 0.8.9, schema 1
     */
    @SerializedName(value = "chestGlint")
    public @NonNull EnableGlint chestGlint;

    /**
     * Whether enchantment glint should be drawn on the legs slot. Also
     * see {@link EnableGlint}.<br/><br/>
     * This was introduced on 8th March 2026 in PR #114.
     *
     * @since AH 0.8.9, schema 1
     */
    @SerializedName(value = "legsGlint")
    public @NonNull EnableGlint legsGlint;

    /**
     * Whether enchantment glint should be drawn on the boot slot. Also
     * see {@link EnableGlint}.<br/><br/>
     * This was introduced on 8th March 2026 in PR #114.
     *
     * @since AH 0.8.9, schema 1
     */
    @SerializedName(value = "bootsGlint")
    public @NonNull EnableGlint bootsGlint;

    /**
     * Whether combat detection (for combat fade-off on transparency) should be enabled or not. Also see {@link CombatDetection}.<br/><br/>
     * This was part of the initial release and is core functionality of Armor Hider.
     *
     * @since AH 0.1.0, schema 1
     */
    @SerializedName(value = "enableCombatDetection")
    public @NonNull CombatDetection enableCombatDetection;

    /**
     * Gets the configuration item {@link OpacityAffectingElytraItem} that determines whether the chest opacity slider should affect Elytra rendering too.
     *
     * @since AH 0.5.0, schema 1
     */
    @SerializedName(value = "opacityAffectingElytra")
    public @NonNull OpacityAffectingElytraItem opacityAffectingElytra;

    /**
     * Whether Armor Hider's helmet opacity {@link PlayerConfig helmetOpacity} should affect skulls.<br/><br/>
     *
     * This setting initially affected hats, but has since changed to only affect skull transparency.<br/><br/>
     *
     * This was introduced in PR#28 on 8th Jan 2026.
     *
     * @since AH 0.5.0, schema 1
     */
    @SerializedName(value = "opacityAffectingHatOrSkull")
    public @NonNull OpacityAffectingHatOrSkullItem opacityAffectingHatOrSkull;

    /**
     * Whether Armor Hider should be disabled globally for the user themselves and for the other players drawn via the {@link DisableArmorHiderGlobally} configuration item.<br/>
     * Also see {@link PlayerConfig#disableArmorHiderForOthers}
     *
     * @since AH 0.6.0, schema 1
     */
    @SerializedName(value = "disableArmorHider", alternate = "globalArmorHiderToggle")
    public @NonNull DisableArmorHiderGlobally disableArmorHider;

    /**
     * Whether Armor Hider should be disabled for other players drawn,
     * via the {@link DisableArmorHiderForOthers} configuration item.
     *
     * @since AH 0.6.0, schema 1
     */
    @SerializedName(value = "disableArmorHiderForOthers", alternate = "toggleArmorHiderForOthers")
    public @NonNull DisableArmorHiderForOthers disableArmorHiderForOthers;

    /**
     * Whether the player's own configuration should be used when an unknown player is probed (i.e. someone not using the mod). Uses the {@link UsePlayerSettingsWhenUndeterminable} configuration item.
     *
     * @since AH 0.6.0, schema 1
     */
    @SerializedName(value = "usePlayerSettingsWhenUndeterminable")
    public @NonNull UsePlayerSettingsWhenUndeterminable usePlayerSettingsWhenUndeterminable;

    /**
     * The opacity value of the offhand item (e.g. shield/totem/...).
     * See: {@link OffHandOpacity}.<br/><br/>
     *
     * This was introduced on 15th February 2026 in 31fda5f888d59fa0c3b33b6484ccbc4a5febec77.
     *
     * @since 0.7.8-pre.1, schema 1
     */
    @SerializedName(value = "offHandOpacity")
    public @NonNull OffHandOpacity offHandOpacity;

    /**
     * A list of exclusion items that should be ignored by Armor Hider.<br/>
     * See {@link ExclusionItemConfiguration},
     *
     * @since AH 0.10.0-pre.5, schema 1
     */
    @SerializedName(value = "exclusionItems")
    public @NonNull ExclusionItemConfiguration exclusionItems;

    /**
     * @since AH 0.10.4-pre.1, schema 2
     */
    @SerializedName(value = "showSettingsInSkinCustomization")
    public @NonNull ShowSettingsInSkinCustomization showSettingsInSkinCustomization;

    /**
     * Gets the configuration item {@link InCombatUseDefaultArmorSkin} that determines whether the player should use the default armor skin in combat instead of one provided by resource packs.
     *
     * @since AH 0.10.18-pre.1 schema 4
     */
    @SerializedName(value = "inCombatUseDefaultModel")
    public @NonNull InCombatUseDefaultArmorSkin inCombatUseDefaultModel;

    /**
     * Whether the shield should be drawn at full opacity when the player is blocking, defined via the {@link ShowShieldWhenBlocking} configuration item.<br/><br/>
     *
     * This was initially released in AH v0.11.4-pre.1 on 6th Feb 2026.
     *
     * @since AH 0.11.4-pre.1, schema 1
     */
    @SerializedName(value = "showShieldWhenBlocking")
    public @NonNull ShowShieldWhenBlocking showShieldWhenBlocking;


    /**
     * Whether armor hider's functionality gets disabled when the player is invisible (via the invisibility effect).
     * This can prevent players getting fully invisible via Armor Hider while still wearing armor, which can be used as a competitive advantage.
     *
     * @since AH 0.12.0-pre.5, schema 5
     */
    @SerializedName(value = "disableArmorHiderOnInvisibility")
    public @NonNull DisableArmorHiderOnInvisibility disableArmorHiderOnInvisibility;

    /**
     * Represents the per-player configuration mappings associated with individual servers.
     * This field is used to store and manage user-specific configurations for multiple players
     * across different servers. It allows for customization of player configurations on a
     * per-server basis, ensuring that specific overrides or settings can be applied independently for
     * each player-server pair.<br/><br/>
     *
     * Individual configurations stored within this field are excluded when preparing the
     * {@link PlayerConfig} for network transmission. These configurations are intended for
     * client-side use and are not transmitted to the server or other players.<br/><br/>
     *
     * The field leverages {@link ServerMappedIndividualConfigurations}, which provides utilities
     * for adding, removing, and retrieving overrides for specific players on specific servers.
     *
     * @since AH 0.12.0-pre.10, schema 6
     */
    @SerializedName(value = "individualConfigurations")
    public @NonNull ServerMappedIndividualConfigurations individualConfigurations;

    /** Whether the server-independent global override (below) is applied to EVERY other player (Row C).
     *
     * @since AH 0.12.0-pre.10, schema 7
     * */
    @SerializedName(value = "useGlobalOverrideForAllPlayers")
    public @NonNull UseGlobalOverrideForAllPlayers useGlobalOverrideForAllPlayers;

    /**
     * The client-side, server-independent global override configuration applied to unknown players when
     * {@link #useGlobalOverrideForAllPlayers} is on. Lazily created (left {@code null} until the user enables it) so
     * the no-arg constructor doesn't recurse — a {@code PlayerConfig} field that always built another
     * {@code PlayerConfig} would never terminate. Nested overrides never populate their own, so
     * serialization terminates too.
     *
     * @since AH 0.12.0-pre.10, schema 8
     */
    @SerializedName(value = "globalPlayerOverride")
    public @org.jetbrains.annotations.Nullable PlayerConfig globalPlayerOverride;

    public @NonNull PlayerUuid playerId;

    public @NonNull PlayerName playerName;

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
        inCombatUseDefaultModel = new InCombatUseDefaultArmorSkin();
        enableCombatDetection = new CombatDetection();
        playerId = new PlayerUuid();
        playerName = new PlayerName();
        opacityAffectingHatOrSkull = new OpacityAffectingHatOrSkullItem();
        opacityAffectingElytra = new OpacityAffectingElytraItem();
        disableArmorHider = new DisableArmorHiderGlobally();
        disableArmorHiderForOthers = new DisableArmorHiderForOthers();
        usePlayerSettingsWhenUndeterminable = new UsePlayerSettingsWhenUndeterminable();
        showSettingsInSkinCustomization = new ShowSettingsInSkinCustomization();
        offHandOpacity = new OffHandOpacity();
        showShieldWhenBlocking = new ShowShieldWhenBlocking();
        helmetGlint = new EnableGlint();
        chestGlint = new EnableGlint();
        legsGlint = new EnableGlint();
        bootsGlint = new EnableGlint();
        disableArmorHiderOnInvisibility = new DisableArmorHiderOnInvisibility();
        individualConfigurations = new ServerMappedIndividualConfigurations();
        useGlobalOverrideForAllPlayers = new UseGlobalOverrideForAllPlayers();
        // globalPlayerOverride is intentionally left null here (lazy) to avoid infinite ctor recursion.
        exclusionItems = ExclusionItemConfiguration.defaults();
    }

    public @NonNull ExclusionItemConfiguration getExclusionItems() {
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
        fresh.inCombatUseDefaultModel.setValue(old.inCombatUseDefaultModel.getValue());
        fresh.opacityAffectingElytra.setValue(old.opacityAffectingElytra.getValue());
        fresh.opacityAffectingHatOrSkull.setValue(old.opacityAffectingHatOrSkull.getValue());
        fresh.disableArmorHider.setValue(old.disableArmorHider.getValue());
        fresh.disableArmorHiderForOthers.setValue(old.disableArmorHiderForOthers.getValue());
        fresh.usePlayerSettingsWhenUndeterminable.setValue(old.usePlayerSettingsWhenUndeterminable.getValue());
        fresh.showSettingsInSkinCustomization.setValue(old.showSettingsInSkinCustomization.getValue());
        fresh.offHandOpacity.setValue(old.offHandOpacity.getValue());
        fresh.showShieldWhenBlocking.setValue(old.showShieldWhenBlocking.getValue());
        fresh.disableArmorHiderOnInvisibility.setValue(old.disableArmorHiderOnInvisibility.getValue());
        fresh.exclusionItems = old.exclusionItems.deepCopy();
        if (old.individualConfigurations != null) {
            fresh.individualConfigurations = old.individualConfigurations.deepCopy();
        }
        fresh.useGlobalOverrideForAllPlayers.setValue(old.useGlobalOverrideForAllPlayers.getValue());
        if (old.globalPlayerOverride != null) {
            fresh.globalPlayerOverride = old.globalPlayerOverride.deepCopy(
                    old.globalPlayerOverride.playerName.getValue(), old.globalPlayerOverride.playerId.getValue());
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

    @Override
    public int getSchemaVersion() {
        return configVersion;
    }

    @Override
    public int getCurrentSchemaVersion() {
        return CURRENT_CONFIG_VERSION;
    }

    @Override
    public PlayerConfig migrateFrom(PlayerConfig old) {
        return PlayerConfig.migrate(old);
    }

    public String toJson() {
        return ArmorHider.GSON.toJson(this);
    }

    /**
     * Returns a copy of this config suitable for transmission to the server. It carries every
     * render-relevant setting but deliberately omits all of the viewer's client-only "how I view others"
     * state: {@link #individualConfigurations}, the {@link #useGlobalOverrideForAllPlayers} flag and the
     * {@link #globalPlayerOverride}. Those are a purely client-side concern and must never be broadcast to
     * the server or other players. ({@link #deepCopy} copies none of them, so this delegates straight to it.)
     */
    public PlayerConfig forNetwork() {
        return deepCopy(playerName.getValue(), playerId.getValue());
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
        newConfig.inCombatUseDefaultModel.setValue(this.inCombatUseDefaultModel.getValue());
        newConfig.opacityAffectingHatOrSkull.setValue(this.opacityAffectingHatOrSkull.getValue());
        newConfig.opacityAffectingElytra.setValue(this.opacityAffectingElytra.getValue());
        newConfig.usePlayerSettingsWhenUndeterminable.setValue(this.usePlayerSettingsWhenUndeterminable.getValue());
        newConfig.showSettingsInSkinCustomization.setValue(this.showSettingsInSkinCustomization.getValue());
        newConfig.offHandOpacity.setValue(this.offHandOpacity.getValue());
        newConfig.showShieldWhenBlocking.setValue(this.showShieldWhenBlocking.getValue());
        newConfig.disableArmorHiderOnInvisibility.setValue(this.disableArmorHiderOnInvisibility.getValue());
        newConfig.helmetGlint.setValue(this.helmetGlint.getValue());
        newConfig.chestGlint.setValue(this.chestGlint.getValue());
        newConfig.legsGlint.setValue(this.legsGlint.getValue());
        newConfig.bootsGlint.setValue(this.bootsGlint.getValue());
        newConfig.exclusionItems = this.getExclusionItems().deepCopy();
        return newConfig;
    }
}
