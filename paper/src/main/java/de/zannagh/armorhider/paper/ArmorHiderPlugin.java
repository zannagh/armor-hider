package de.zannagh.armorhider.paper;

import de.zannagh.armorhider.paper.config.LevelRootLocator;
import de.zannagh.armorhider.paper.config.ServerConfigStorage;
import de.zannagh.armorhider.paper.config.ServerConfigurationState;
import de.zannagh.armorhider.paper.net.ChannelSubscriber;
import de.zannagh.armorhider.paper.net.Channels;
import de.zannagh.armorhider.paper.net.ClientDialects;
import de.zannagh.armorhider.paper.net.PacketSender;
import de.zannagh.armorhider.paper.perm.PermissionResolver;
import de.zannagh.armorhider.paper.util.Schedulers;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.io.File;
import java.util.List;

/**
 * Server-side Armor Hider, reimplemented as a Bukkit plugin.
 *
 * <p>A schema-agnostic relay: it stores per-player configs opaquely, keeps the four server-wide
 * booleans, and forwards combat-log events. Because it touches no NMS and no version-specific API,
 * one jar serves every game version the mod supports.</p>
 */
public final class ArmorHiderPlugin extends JavaPlugin {

    private ArmorHiderService service;

    @Override
    public void onEnable() {
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
        ClientDialects dialects = new ClientDialects();
        PacketSender sender = new PacketSender(this, dialects);
        service = new ArmorHiderService(getLogger(), state, storage, sender, permissions, schedulers);

        registerChannels(Channels.ALL, dialects);
        ChannelSubscriber subscriber = new ChannelSubscriber(getLogger(), Channels.ALL);
        getServer().getPluginManager()
                .registerEvents(new PlayerConnectionListener(service, subscriber, dialects), this);

        getLogger().info("Armor Hider server relay enabled (folia=" + schedulers.isFolia()
                + ", luckperms=" + permissions.isLuckPermsPresent() + ").");
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
     * Registers every channel as both incoming and outgoing.
     *
     * <p>Outgoing registration is what lets the plugin send at all; incoming registration is
     * additionally required because Paper only advertises {@code messenger.getIncomingChannels()}
     * in the server's own REGISTER, and only once, during {@code PlayerList.placeNewPlayer} - so a
     * channel registered later would never be announced.</p>
     */
    private void registerChannels(List<String> channels, ClientDialects dialects) {
        Messenger messenger = getServer().getMessenger();
        ArmorHiderMessageListener listener =
                new ArmorHiderMessageListener(getLogger(), service, dialects);
        for (String channel : channels) {
            messenger.registerOutgoingPluginChannel(this, channel);
            messenger.registerIncomingPluginChannel(this, channel, listener);
        }
    }
}
