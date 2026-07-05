package de.zannagh.armorhider.net.packets;

import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.configuration.ConfigurationSource;
import de.zannagh.armorhider.configuration.items.CombatDetection;
import de.zannagh.armorhider.configuration.items.DisableArmorHiderOnInvisibilityGlobally;
import de.zannagh.armorhider.configuration.items.ForceArmorHiderOffOnPlayers;
//? if >= 1.20.5 {
import de.zannagh.armorhider.net.CompressedJsonCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import net.minecraft.resources.Identifier;

public class ServerWideSettings implements ConfigurationSource<ServerWideSettings> {

    /**
     * Config schema version. Absent (0) in configs from before versioning was introduced.
     * Incremented when the structure changes in a way that requires migration.
     * <ul>
     *   <li>0 = pre-versioning format (initial v4 ServerConfiguration shape — only enableCombatDetection + forceArmorHiderOff)</li>
     *   <li>1 = added disableArmorHiderOnInvisibilityGlobally</li>
     * </ul>
     */
    @SerializedName(value = "configVersion")
    public int configVersion;

    /** The current config schema version. */
    public static final int CURRENT_CONFIG_VERSION = 1;

    //? if >= 1.21.11 {
    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "server_wide_settings");
     //?}
    //? if >= 1.20.5 && < 1.21.11 {
    /*public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("armorhider", "server_wide_settings");
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
    @NonNull
    public CombatDetection enableCombatDetection;
    @SerializedName(value = "forceArmorHiderOff")
    @NonNull
    public ForceArmorHiderOffOnPlayers forceArmorHiderOff;
    @SerializedName(value = "disableArmorHiderOnInvisibilityGlobally")
    @NonNull
    public DisableArmorHiderOnInvisibilityGlobally disableArmorHiderOnInvisibilityGlobally;

    private transient boolean hasChangedFromSerializedContent = false;

    public ServerWideSettings() {
        this.enableCombatDetection = new CombatDetection();
        this.forceArmorHiderOff = new ForceArmorHiderOffOnPlayers();
        this.disableArmorHiderOnInvisibilityGlobally = new DisableArmorHiderOnInvisibilityGlobally();
    }

    public ServerWideSettings(Boolean enableCombatDetection, Boolean forceArmorHiderOff, Boolean disableArmorHiderOnInvisibilityGlobally) {
        this();
        this.enableCombatDetection = new CombatDetection(enableCombatDetection);
        this.forceArmorHiderOff = new ForceArmorHiderOffOnPlayers(forceArmorHiderOff);
        this.disableArmorHiderOnInvisibilityGlobally = new DisableArmorHiderOnInvisibilityGlobally(disableArmorHiderOnInvisibilityGlobally);
        this.configVersion = CURRENT_CONFIG_VERSION;
    }

    @Contract("-> new")
    public static @NonNull ServerWideSettings defaults() {
        var fresh = new ServerWideSettings();
        fresh.configVersion = CURRENT_CONFIG_VERSION;
        return fresh;
    }

    /**
     * Migrates settings from an older schema version to the current one.
     * Creates a fresh instance with all current defaults, then overlays
     * the values that were present in the old payload.
     */
    public static @NonNull ServerWideSettings migrate(@NonNull ServerWideSettings old) {
        ArmorHider.LOGGER.info("Migrating server-wide settings from version {} to {}.",
                old.configVersion, CURRENT_CONFIG_VERSION);

        var fresh = defaults();
        fresh.enableCombatDetection.setValue(old.enableCombatDetection.getValue());
        fresh.forceArmorHiderOff.setValue(old.forceArmorHiderOff.getValue());
        fresh.disableArmorHiderOnInvisibilityGlobally.setValue(old.disableArmorHiderOnInvisibilityGlobally.getValue());
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
}
