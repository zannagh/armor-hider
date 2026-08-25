package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderPlayerConfigApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.utils.McClientUtils;
import de.zannagh.armorhider.configuration.ConfigurationProvider;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.AhPackets;
import de.zannagh.armorhider.net.packets.AhReplicatedPlayerConfig;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.server.ServerConfiguration;
import de.zannagh.eunomia.keyed.ReplicatedClientStore;
import de.zannagh.eunomia.networking.comms.CommunicationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class AhPlayerConfigApiImpl implements ArmorHiderPlayerConfigApi, ConfigurationProvider<PlayerConfig> {

    private final ConfigurationProvider<PlayerConfig> playerConfigProvider;

    private PlayerConfig CURRENT = PlayerConfig.defaults(DEFAULT_PLAYER_ID, DEFAULT_PLAYER_NAME);

    private @Nullable ServerConfiguration serverConfiguration;

    // Client-side mirror of the relay's per-player config store, used ONLY on the HTTP/WebSocket fallback
    // path (server does not run the mod). It is populated by eunomia's relay (snapshot on join + live
    // relays); on a normal armor-hider MC server it stays empty, so the real SERVER_CONFIG snapshot is never
    // shadowed. See currentServerConfig().
    private @Nullable ReplicatedClientStore<AhReplicatedPlayerConfig> relayMirror;

    private final HashMap<UUID, Consumer<@Nullable String>> configListeners = new HashMap<>();

    public AhPlayerConfigApiImpl() {
        this.playerConfigProvider = new de.zannagh.armorhider.configuration.PlayerConfigFileProvider();
        CURRENT = load();
    }

    public AhPlayerConfigApiImpl(ConfigurationProvider<PlayerConfig> configurationProvider) {
        this.playerConfigProvider = configurationProvider;
        CURRENT = load();
    }

    @Override
    public UUID addConfigChangeListener(Consumer<@Nullable String> listener) {
        var uuid = UUID.randomUUID();
        configListeners.put(uuid, listener);
        return uuid;
    }

    @Override
    public void removeConfigChangeListener(UUID listenerGuid) {
        configListeners.remove(listenerGuid);
    }

    @Override
    public void notifyConfigListeners(@Nullable String playerName) {
        // Iterate a snapshot: a listener may (de)register listeners while being notified, which would
        // otherwise throw a ConcurrentModificationException or skip listeners.
        for (Consumer<@Nullable String> listener : new java.util.ArrayList<>(configListeners.values())) {
            listener.accept(playerName);
        }
    }

    public PlayerConfig load() {
        return playerConfigProvider.getValue();
    }

    public PlayerConfig getDefault() {
        return ArmorHiderPlayerConfigApi.getDefault();
    }

    public void save(PlayerConfig config) {
        playerConfigProvider.save(config);
        // Record the exact "how I view others" toggle state AND the viewer's own opacity sliders on every
        // persist, together with the config instance's identity, so a user's debug log pins down what actually
        // reached disk (see issue: unknown players / "own settings don't stick"). Comparing the identity and
        // opacity here against the resolveConfig(self=...) line below tells us whether the value a user set in
        // the panel is the value that gets persisted and rendered, or whether an instance got swapped in between.
        if (DebugLogger.isEnabled()) {
            DebugLogger.log(
                    "Persisted local config for {} #{} | H={} C={} L={} B={} O={} | usePlayerSettingsForUnknown={} useGlobalOverrideForAll={} disableForOthers={} disableGlobally={} globalOverrideSet={} serverConfigPresent={}",
                    config.playerName.getValue(),
                    System.identityHashCode(config),
                    config.helmetOpacity.getValue(),
                    config.chestOpacity.getValue(),
                    config.legsOpacity.getValue(),
                    config.bootsOpacity.getValue(),
                    config.offHandOpacity.getValue(),
                    config.usePlayerSettingsWhenUndeterminable.getValue(),
                    config.useGlobalOverrideForAllPlayers.getValue(),
                    config.disableArmorHiderForOthers.getValue(),
                    config.disableArmorHider.getValue(),
                    config.globalPlayerOverride != null,
                    serverConfiguration != null);
        }

        ClientPacketListener clientNetwork = Minecraft.getInstance().getConnection();
        if (serverConfiguration != null && McClientUtils.isClientConnectedToServer() && clientNetwork != null) {
            ArmorHider.LOGGER.info("Sending to server...");
            // Strip the client-only per-player override map before transmitting (privacy + irrelevant to peers).
            CommunicationManager.sendToServer(AhPackets.PLAYER_CONFIG, config.forNetwork());
            ArmorHider.LOGGER.info("Send client config package to server.");
        }
        // Also publish over the replicated channel so config propagates on the HTTP/WebSocket fallback (where
        // no mod server exists and the block above never fires). eunomia's send gate routes this: to the relay
        // when the fallback is active, or dropped on a plain server with no relay - so it is safe to fire on any
        // connection. Unhandled (harmless) on a normal armor-hider MC server, which uses PLAYER_CONFIG above.
        if (McClientUtils.isClientConnectedToServer() && clientNetwork != null) {
            UUID id = config.playerId.getValue();
            if (id != null) {
                CommunicationManager.sendToServer(
                        AhPackets.PLAYER_CONFIG_REPLICATED, AhReplicatedPlayerConfig.forNetwork(id, config));
            }
        }
        notifyConfigListeners(config.playerName.getValue());
    }

    @Override
    public void saveCurrent() {
        save(CURRENT);
    }

    @Override
    public PlayerConfig getValue() {
        return CURRENT;
    }

    @Override
    public void setValue(PlayerConfig newValue) {
        CURRENT = newValue;
        save(CURRENT);
    }

    @Override
    public void updateLocalPlayerName(String playerName, Optional<Boolean> withSave) {
        CURRENT.playerName.setValue(playerName);
        // Per the documented contract, saving defaults to enabled when withSave is empty.
        if (withSave.orElse(true)) {
            save(CURRENT);
        }
    }

    @Override
    public void updateLocalPlayerUuid(UUID id, Optional<Boolean> withSave) {
        CURRENT.playerId.setValue(id);
        // Per the documented contract, saving defaults to enabled when withSave is empty.
        if (withSave.orElse(true)) {
            save(CURRENT);
        }
    }

    @Override
    public void saveLocalPlayerConfig(PlayerConfig config) {
        // Adopt the given config as CURRENT before persisting, so getLocalPlayerConfig()/resolveConfig()
        // don't keep returning a stale instance when a caller passes a freshly-built config rather than the
        // mutated-in-place CURRENT. For the in-place callers this is a harmless self-assignment.
        CURRENT = config;
        save(CURRENT);
    }

    @Override
    public PlayerConfig getLocalPlayerConfig() {
        return CURRENT;
    }

    @Override
    public void setLocalPlayerConfig(PlayerConfig config) {
        CURRENT = config;
        save(CURRENT);
    }

    @Override
    public void setAndSendServerWideSettings(@NonNull ServerWideSettings serverWideSettings) {
        if (ArmorHiderClient.permissionLevel < 3) {
            ArmorHider.LOGGER.info("Player is no admin, suppressing update...");
            return;
        }
        if (serverConfiguration == null) {
            return;
        }
        serverConfiguration.serverWideSettings = serverWideSettings;
        ArmorHider.LOGGER.info("Sending server-wide settings to server...");
        CommunicationManager.sendToServer(AhPackets.SERVER_WIDE_SETTINGS, serverWideSettings);
    }

    @Override
    public void setAndSendServerConfig(boolean combatDetection, boolean forceArmorHiderOff, boolean disableArmorHiderOnInvisibilityGlobally, boolean allowIndividualPlayerConfigurations) {
        if (ArmorHiderClient.permissionLevel < 3) {
            return;
        }
        if (serverConfiguration == null) {
            return;
        }
        serverConfiguration.serverWideSettings.enableCombatDetection.setValue(combatDetection);
        serverConfiguration.serverWideSettings.forceArmorHiderOff.setValue(forceArmorHiderOff);
        serverConfiguration.serverWideSettings.disableArmorHiderOnInvisibilityGlobally.setValue(disableArmorHiderOnInvisibilityGlobally);
        serverConfiguration.serverWideSettings.allowIndividualPlayerConfigurations.setValue(allowIndividualPlayerConfigurations);
        setAndSendServerWideSettings(serverConfiguration.serverWideSettings);
    }

    /**
     * Installs the client-side relay mirror for the HTTP/WebSocket fallback. Idempotent; call once from client
     * init after eunomia is ready. On the fallback path the relay pushes every player's config here (snapshot on
     * join + live relays); {@link #currentServerConfig()} surfaces them through the normal resolution path.
     */
    @Override
    public void enableRelayMirror() {
        if (relayMirror == null) {
            CommunicationManager.register(AhPackets.PLAYER_CONFIG_REPLICATED);
            relayMirror = new ReplicatedClientStore<>(
                    1, AhReplicatedPlayerConfig.class, AhPackets.PLAYER_CONFIG_REPLICATED).enableClient();
        }
    }

    /**
     * The per-player config snapshot resolution should read: the real server-transmitted {@link ServerConfiguration}
     * when a mod server sent one, otherwise a view rebuilt from the relay mirror when the fallback populated it.
     * The two are mutually exclusive - a mod server sends SERVER_CONFIG and never the replicated store; the relay
     * populates the mirror and never sends SERVER_CONFIG - so this never shadows a real snapshot with relay data.
     */
    private @Nullable ServerConfiguration currentServerConfig() {
        if (serverConfiguration != null) {
            return serverConfiguration;
        }
        if (relayMirror == null) {
            return null;
        }
        var snapshot = relayMirror.store().snapshot();
        if (snapshot.isEmpty()) {
            return null;
        }
        ServerConfiguration view = new ServerConfiguration();
        for (AhReplicatedPlayerConfig entry : snapshot.values()) {
            PlayerConfig config = entry.toPlayerConfig();
            if (config != null) {
                view.put(config);
            }
        }
        return view;
    }

    @Override
    public @Nullable ServerConfiguration getServerConfig() {
        return currentServerConfig();
    }

    @Override
    public void setServerConfig(ServerConfiguration serverConfig) {
        ArmorHider.LOGGER.info("Setting server config...");
        serverConfiguration = serverConfig;
        notifyConfigListeners(null);
    }

    @Override
    public void clearServerConfig() {
        ArmorHider.LOGGER.info("Clearing server config...");
        serverConfiguration = null;
        notifyConfigListeners(null);
    }

    @Override
    public @Nullable String getServerKey() {
        var server = Minecraft.getInstance().getCurrentServer();
        if (server == null) {
            return "singleplayer";
        }
        if (server.ip != null && !server.ip.isBlank()) {
            return server.ip;
        }
        return server.name != null ? server.name : "unknown";
    }

    // Diagnostic-only: the last (identity + opacity) signature emitted by logOwnResolveForDiagnostics, so the
    // render-hot resolveConfig path emits a self-resolution line only when it actually changes.
    private static String lastOwnResolveSignature;

    /**
     * Records, for a user's debug log, the opacity the render pipeline resolves for the <b>local</b> player and
     * the identity of the config instance it read them from. resolveConfig runs every frame for every player,
     * so this is change-gated: it only emits when the signature differs from the previous emission, keeping it
     * to a handful of lines. Pair it with the {@code Persisted local config ... #<id>} line from {@link #save}
     * to confirm whether a user's own persisted opacity actually reaches rendering, and from the same instance.
     */
    private void logOwnResolveForDiagnostics(String playerName) {
        if (!DebugLogger.isEnabled()) {
            // Clear the de-dupe signature while logging is off, so re-enabling a debug log always emits a fresh
            // self-resolution line even if the opacities haven't changed since the previous debug session.
            lastOwnResolveSignature = null;
            return;
        }
        String signature = System.identityHashCode(CURRENT)
                + ":" + CURRENT.helmetOpacity.getValue()
                + ":" + CURRENT.chestOpacity.getValue()
                + ":" + CURRENT.legsOpacity.getValue()
                + ":" + CURRENT.bootsOpacity.getValue()
                + ":" + CURRENT.offHandOpacity.getValue();
        if (signature.equals(lastOwnResolveSignature)) {
            return;
        }
        lastOwnResolveSignature = signature;
        DebugLogger.log(
                "resolveConfig(self='{}') -> local #{} | H={} C={} L={} B={} O={}",
                playerName,
                System.identityHashCode(CURRENT),
                CURRENT.helmetOpacity.getValue(),
                CURRENT.chestOpacity.getValue(),
                CURRENT.legsOpacity.getValue(),
                CURRENT.bootsOpacity.getValue(),
                CURRENT.offHandOpacity.getValue());
    }

    @Override
    public PlayerConfig resolveConfig(@Nullable String playerName) {
        if (playerName != null && playerName.equals(ArmorHiderClient.getCurrentPlayerName())) {
            logOwnResolveForDiagnostics(playerName);
            return CURRENT;
        }

        boolean allowed = areOtherPlayerConfigsAllowed();

        // The server-transmitted snapshot on the normal path, or the relay mirror's view on the fallback path.
        ServerConfiguration serverSnapshot = currentServerConfig();

        // Null name means the player couldn't be identified - treat as unknown/undeterminable.
        if (playerName == null) {
            return allowed
                    ? resolveUnknownPlayerConfig(DEFAULT_PLAYER_NAME, DEFAULT_PLAYER_ID)
                    : PlayerConfig.defaults(DEFAULT_PLAYER_ID, DEFAULT_PLAYER_NAME);
        }

        // A mod server that disallows client-side other-player config makes all the client controls inert:
        // fall back to the server-transmitted config (if any) or vanilla defaults.
        if (!allowed) {
            var serverConfig = serverSnapshot != null
                    ? serverSnapshot.getPlayerConfigOrDefault(playerName) : null;
            return serverConfig != null ? serverConfig : PlayerConfig.defaults(DEFAULT_PLAYER_ID, playerName);
        }

        // Individual per-player override (requires a mod server that allows it) wins over everything below.
        if (areIndividualConfigsAllowedByServer()) {
            PlayerConfig override = CURRENT.individualConfigurations.getOverride(getServerKey(), playerName);
            if (override != null) {
                return override;
            }
        }

        // Row C: "use the global configuration for all other players" overrides even server-broadcast configs.
        if (CURRENT.useGlobalOverrideForAllPlayers.getValue()) {
            return getGlobalConfigOverride();
        }

        // Server-transmitted config from a modded player, by name then by UUID.
        UUID playerId = DEFAULT_PLAYER_ID;
        if (serverSnapshot != null) {
            var config = serverSnapshot.getPlayerConfigOrDefault(playerName);
            if (config != null) {
                return config;
            }
            var isRemotePlayer = ArmorHiderClient.isPlayerRemotePlayer(playerName);
            if (isRemotePlayer.getA()) {
                //? if >= 1.21.9 {
                playerId = isRemotePlayer.getB().getProfile().id();
                config = serverSnapshot.getPlayerConfigOrDefault(isRemotePlayer.getB().getProfile().id());
                //?}
                //? if < 1.21.9 {
                /*playerId = isRemotePlayer.getB().getProfile().getId();
                config = serverSnapshot.getPlayerConfigOrDefault(isRemotePlayer.getB().getProfile().getId());
                *///?}
                if (config != null) {
                    return config;
                }
            }
        }

        // Unknown player (no server config) - Row B decides the source.
        return resolveUnknownPlayerConfig(playerName, playerId);
    }

    @Override
    public PlayerConfig resolveUnknownPlayerConfig(String playerName, UUID playerId) {
        if (CURRENT.usePlayerSettingsWhenUndeterminable.getValue()) {
            return CURRENT.deepCopy(playerName, playerId);
        }
        return getGlobalConfigOverride();
    }
}
