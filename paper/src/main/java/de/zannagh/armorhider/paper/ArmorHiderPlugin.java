package de.zannagh.armorhider.paper;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import de.zannagh.armorhider.paper.config.LevelRootLocator;
import de.zannagh.armorhider.paper.config.ServerConfigStorage;
import de.zannagh.armorhider.paper.config.ServerConfigurationState;
import de.zannagh.armorhider.paper.net.ArmorHiderPaperPackets;
import de.zannagh.armorhider.paper.net.ArmorHiderServerContext;
import de.zannagh.armorhider.paper.net.ChannelSubscriber;
import de.zannagh.armorhider.paper.net.PaperServerTransport;
import de.zannagh.armorhider.paper.perm.PermissionResolver;
import de.zannagh.armorhider.paper.util.Schedulers;
import de.zannagh.eunomia.networking.comms.CommunicationManager;
import de.zannagh.eunomia.networking.packets.PacketType;
import de.zannagh.eunomia.networking.packets.ServerContext;
import de.zannagh.eunomia.networking.serialization.NetworkSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Server-side Armor Hider, reimplemented as a Bukkit plugin over eunomia's networking core.
 *
 * <p>A schema-agnostic relay: it stores per-player configs opaquely as {@link JsonObject}s, keeps the
 * four server-wide booleans, and forwards combat-log events. It speaks eunomia's clean wire frame
 * (bare {@code gzip(json)}, no length prefix) on the single {@code de.zannagh.armorhider} namespace,
 * so one jar - which touches no NMS and no version-specific API - serves every game version the mod
 * supports and interoperates byte-for-byte with the modded client.</p>
 */
public final class ArmorHiderPlugin extends JavaPlugin {

    private ArmorHiderService service;
    private PaperServerTransport transport;

    @Override
    public void onEnable() {
        // Install the wire/disk Gson before anything (de)serializes. Pretty-printing matches
        // ArmorHider.GSON, so configs round-trip byte-for-byte between the mod and this plugin, and
        // the shared PayloadCodec resolves opaque JsonObject payloads with it.
        NetworkSerializer.setGson(new GsonBuilder().setPrettyPrinting().create());

        ServerConfigStorage storage = new ServerConfigStorage(
                LevelRootLocator.configFile(getDataFolder()),
                new File("config", "armor-hider-server.json").toPath(),
                getLogger());
        storage.migrateDimensionFolderConfigIfNeeded(
                LevelRootLocator.strandedDimensionConfigFile(getDataFolder()));
        storage.migrateGlobalConfigIfNeeded();
        ServerConfigurationState state = storage.load();

        Schedulers schedulers = new Schedulers(this);
        PermissionResolver permissions = new PermissionResolver(getLogger());
        service = new ArmorHiderService(getLogger(), state, storage, permissions, schedulers);

        transport = new PaperServerTransport(this);
        CommunicationManager.setServerTransport(transport);

        registerPackets();
        // Answer capability probes so a client detects this Paper server speaks armor-hider's
        // protocol - the eunomia handshake replaces the old handshake_s2c_packet push entirely.
        CommunicationManager.enableServerHandshake();

        List<String> clientboundChannels = registerBukkitChannels();
        ChannelSubscriber subscriber = new ChannelSubscriber(getLogger(), clientboundChannels);
        getServer().getPluginManager()
                .registerEvents(new PlayerConnectionListener(service, subscriber), this);

        getLogger().info("Armor Hider server relay enabled (folia=" + schedulers.isFolia()
                + ", luckperms=" + permissions.isLuckPermsPresent() + ", "
                + CommunicationManager.serverboundTypes().size() + " C2S, "
                + CommunicationManager.clientboundTypes().size() + " S2C channels).");
    }

    @Override
    public void onDisable() {
        Messenger messenger = getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(this);
        messenger.unregisterOutgoingPluginChannel(this);
        if (service != null) {
            service.saveNow();
        }
        service = null;
    }

    /**
     * Declares every armor-hider channel with the {@link CommunicationManager} and wires the three
     * serverbound handlers to {@link ArmorHiderService}. The clientbound types are registered too so
     * they appear in {@link CommunicationManager#clientboundTypes()} for channel registration and
     * force-subscribe.
     */
    private void registerPackets() {
        CommunicationManager.onServerReceive(ArmorHiderPaperPackets.PLAYER_CONFIG,
                (payload, context) -> service.handlePlayerConfig(sender(context), payload));
        CommunicationManager.onServerReceive(ArmorHiderPaperPackets.SERVER_WIDE_SETTINGS,
                (payload, context) -> service.handleServerWideSettings(sender(context), payload));
        CommunicationManager.onServerReceive(ArmorHiderPaperPackets.COMBAT_EVENT,
                (payload, context) -> service.handleCombatLogEvent(sender(context), payload));
        CommunicationManager.onServerReceive(ArmorHiderPaperPackets.SHARED_RULES,
                (payload, context) -> service.handleSharedRuleState(sender(context), payload));

        CommunicationManager.register(ArmorHiderPaperPackets.SERVER_CONFIG);
        CommunicationManager.register(ArmorHiderPaperPackets.PERMISSION);
        CommunicationManager.register(ArmorHiderPaperPackets.COMBAT_NOTIFICATION);
        CommunicationManager.register(ArmorHiderPaperPackets.SHARED_RULES_NOTIFICATION);
    }

    /**
     * Registers each serverbound channel as incoming (with the dispatch listener) and each
     * clientbound channel as outgoing, returning the clientbound channel names to force-subscribe.
     *
     * <p>Incoming registration is required beyond the transport's own send path: Paper only
     * advertises {@code messenger.getIncomingChannels()} in the server's own REGISTER, once, during
     * {@code PlayerList.placeNewPlayer}, so a channel registered later would never be announced.</p>
     */
    private List<String> registerBukkitChannels() {
        Messenger messenger = getServer().getMessenger();
        ArmorHiderMessageListener listener = new ArmorHiderMessageListener(getLogger(), transport);
        for (PacketType<?> type : CommunicationManager.serverboundTypes()) {
            messenger.registerIncomingPluginChannel(this, type.channelKey(), listener);
        }
        List<String> clientbound = new ArrayList<>();
        for (PacketType<?> type : CommunicationManager.clientboundTypes()) {
            messenger.registerOutgoingPluginChannel(this, type.channelKey());
            clientbound.add(type.channelKey());
        }
        return clientbound;
    }

    private static Player sender(ServerContext context) {
        return ((ArmorHiderServerContext) context).player();
    }
}
