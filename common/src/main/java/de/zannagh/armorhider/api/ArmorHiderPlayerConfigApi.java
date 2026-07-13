package de.zannagh.armorhider.api;

import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.server.ServerConfiguration;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Client-side access point for Armor Hider's player configurations.
 *
 * <p>An implementation owns three pieces of state:
 * <ul>
 *   <li>the <b>local player's</b> {@link PlayerConfig} (the viewer's own settings, persisted to disk), and</li>
 *   <li>the optional <b>server configuration</b> received from an Armor-Hider-aware server (server-wide
 *       settings + the configs other modded players broadcast), and</li>
 *   <li>a set of <b>change listeners</b> notified whenever a configuration changes, so UI/render state can
 *       react.</li>
 * </ul>
 *
 * <p>The central entry point is {@link #resolveConfig(String)}, which decides — for any player being
 * rendered — which {@link PlayerConfig} applies, honouring the server-wide policy, the viewer's per-player
 * and global overrides, and the "unknown player" fallback. See that method for the full precedence.
 *
 * <p>Most methods here are {@code default} and operate purely on {@link #getLocalPlayerConfig()} /
 * {@link #getServerConfig()}; an implementation only needs to supply state access, persistence, networking,
 * the current-server key, and the two resolution methods.
 *
 * @since 0.12.0
 */
public interface ArmorHiderPlayerConfigApi {

    /** Sentinel name used for the local/default config and the global override config. */
    String DEFAULT_PLAYER_NAME = "dummy";

    /** Sentinel id used for the local/default config and the global override config. */
    UUID DEFAULT_PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    /**
     * Registers a listener notified whenever a player configuration changes (the local config is saved, the
     * server config is set/cleared, or {@link #notifyConfigListeners(String)} is called).
     *
     * @param listener receives the name of the player whose configuration changed, or {@code null} when the
     *                 change is not tied to a specific player (e.g. the server config was received).
     * @return a handle that can later be passed to {@link #removeConfigChangeListener(UUID)}.
     */
    UUID addConfigChangeListener(Consumer<@Nullable String> listener);

    /**
     * Removes a previously-registered listener.
     *
     * @param listenerGuid the handle returned by {@link #addConfigChangeListener(Consumer)}.
     */
    void removeConfigChangeListener(UUID listenerGuid);

    /**
     * Notifies all registered listeners about a configuration change.
     *
     * @param playerName the affected player's name, or {@code null} for a non-player-specific change.
     */
    @ApiStatus.Internal
    void notifyConfigListeners(@Nullable String playerName);

    /**
     * Sets the local player's name and (optionally) persists the change.
     *
     * @param playerName the new name.
     * @param withSave   when present and {@code true}, the local config is saved; defaults to saving.
     */
    void updateLocalPlayerName(String playerName, Optional<Boolean> withSave);

    /**
     * Sets the local player's UUID and (optionally) persists the change.
     *
     * @param id       the new UUID.
     * @param withSave when present and {@code true}, the local config is saved; defaults to saving.
     */
    void updateLocalPlayerUuid(UUID id, Optional<Boolean> withSave);

    /** Persists the given config as the local player's config (writing to disk and syncing to the server). */
    void saveLocalPlayerConfig(PlayerConfig config);

    /** @return the local player's current configuration (the viewer's own settings). */
    PlayerConfig getLocalPlayerConfig();

    /** Replaces the local player's configuration and persists it. */
    void setLocalPlayerConfig(PlayerConfig config);

    /** Persists the current local player configuration. */
    default void saveCurrent() {
        saveLocalPlayerConfig(getLocalPlayerConfig());
    }

    /**
     * Marks the local configuration as changed and notifies listeners without necessarily persisting — used
     * by UI that mutates the config in place and saves separately on close.
     */
    default void markLocalDirty() {
        notifyConfigListeners(getLocalPlayerConfig().playerName.getValue());
    }

    /** @return a fresh, empty default configuration. */
    static PlayerConfig getDefault() {
        return PlayerConfig.empty();
    }

    /** Applies (admin only) and broadcasts the given server-wide settings to the connected Armor-Hider server. */
    void setAndSendServerWideSettings(@NonNull ServerWideSettings serverWideSettings);

    /** Convenience for {@link #setAndSendServerWideSettings} that sets the four server-wide toggles first (admin only). */
    void setAndSendServerConfig(boolean combatDetection, boolean forceArmorHiderOff, boolean disableArmorHiderOnInvisibilityGlobally, boolean allowIndividualPlayerConfigurations);

    /** @return the server configuration received from an Armor-Hider server, or {@code null} if none (e.g. a vanilla server). */
    @Nullable ServerConfiguration getServerConfig();

    /** Stores the server configuration received from an Armor-Hider server and notifies listeners. */
    void setServerConfig(ServerConfiguration serverConfig);

    /** Clears the stored server configuration (e.g. on disconnect) and notifies listeners. */
    void clearServerConfig();

    /**
     * @return a stable key identifying the current server, used as the outer key of the per-player override
     *         map (typically the connection address, falling back to the name, or a single-player sentinel).
     */
    @Nullable String getServerKey();

    /**
     * @return whether individual per-player configurations are permitted: {@code true} when there is no
     *         Armor-Hider server (nothing to disallow it) or the server explicitly allows it.
     */
    default boolean areIndividualConfigsAllowedByServer(){
        var serverConfig = getServerConfig();
        if (serverConfig == null) {
            return true;
        }
        return serverConfig.serverWideSettings.allowIndividualPlayerConfigurations.getValue();
    }

    /**
     * @return whether any client-side configuration of other players applies at all. The server-wide
     *         force-off is a separate, higher-priority guard applied at render time.
     */
    default boolean areOtherPlayerConfigsAllowed() {
        return areIndividualConfigsAllowedByServer();
    }

    /**
     * Sets whether the global override configuration applies to every other player (Row C) and optionally
     * persists the change.
     */
    default void setUseGlobalOverrideForAllPlayersTo(boolean value, Optional<Boolean> withSave) {
        var local = getLocalPlayerConfig();
        local.useGlobalOverrideForAllPlayers.setValue(value);
        if (withSave.orElse(true)) {
            saveLocalPlayerConfig(local);
        }
    }

    /**
     * @return the viewer's stored per-player override for the named player on the current server, or
     *         {@code null} if none exists.
     */
    default @Nullable PlayerConfig getIndividualConfigOverride(String playerName) {
        var local = getLocalPlayerConfig();
        return local.individualConfigurations.getOverride(getServerKey(), playerName);
    }

    /** @return the global override configuration, or a fresh vanilla default if it has not been created yet. */
    default PlayerConfig getGlobalConfigOverride() {
        var local = getLocalPlayerConfig();
        return local.globalPlayerOverride != null
                ? local.globalPlayerOverride
                : PlayerConfig.defaults(DEFAULT_PLAYER_ID, DEFAULT_PLAYER_NAME);
    }

    /** @return the global override configuration, creating and persisting a default one on first use. */
    default PlayerConfig ensureGlobalOverride() {
        var local = getLocalPlayerConfig();
        if (local.globalPlayerOverride == null) {
            local.globalPlayerOverride = PlayerConfig.defaults(DEFAULT_PLAYER_ID, DEFAULT_PLAYER_NAME);
            saveLocalPlayerConfig(local);
        }
        return local.globalPlayerOverride;
    }

    /** @return whether the viewer has a per-player override stored for the named player on the current server. */
    default boolean hasIndividualOverride(String playerName){
        var local = getLocalPlayerConfig();
        return local.individualConfigurations.hasOverride(getServerKey(), playerName);
    }

    /**
     * @return whether other players should be rendered vanilla (Row A "Others: Vanilla"). This is also
     *         {@code true} while the server forces Armor Hider off, so callers reading it as UI state see the
     *         effective outcome.
     */
    default boolean isArmorHiderDisableForOthers() {
        var server = getServerConfig();
        if (server != null && server.serverWideSettings.forceArmorHiderOff.getValue()) {
            return true;
        }
        var local = getLocalPlayerConfig();
        return local.disableArmorHiderForOthers.getValue();
    }

    /**
     * Stores a per-player override for the named player on the current server and optionally persists it.
     *
     * @param playerName the target player's name.
     * @param config     the override configuration to store.
     * @param withSave   when present and {@code true}, the local config is saved.
     */
    default void putIndividualConfigOverride(String playerName, PlayerConfig config, Optional<Boolean> withSave) {
        var local = getLocalPlayerConfig();
        local.individualConfigurations.putOverride(getServerKey(), playerName, config);
        if (withSave.isPresent() && withSave.get()) {
            saveLocalPlayerConfig(local);
        }
    }

    /**
     * Removes the per-player override for the named player on the current server and optionally persists it.
     *
     * @param playerName the target player's name.
     * @param withSave   when present and {@code true}, the local config is saved.
     */
    default void removeIndividualOverride(String playerName, Optional<Boolean> withSave) {
        var local = getLocalPlayerConfig();
        local.individualConfigurations.removeOverride(getServerKey(), playerName);
        if (withSave.isPresent() && withSave.get()) {
            saveLocalPlayerConfig(local);
        }
    }

    /**
     * Sets whether Armor Hider is disabled (rendered vanilla) for other players (Row A) and optionally
     * persists the change.
     */
    default void setArmorHiderDisabledForOthersTo(boolean value, Optional<Boolean> withSave) {
        var local = getLocalPlayerConfig();
        local.disableArmorHiderForOthers.setValue(value);
        if (withSave.isPresent() && withSave.get()) {
            saveLocalPlayerConfig(local);
        }
    }

    /**
     * @return whether unknown (non-mod) players should use the viewer's own settings (Row B). Returns
     *         {@code false} — meaning the global configuration would be used — when a server disallows
     *         individual configurations, matching how the UI presents that state.
     */
    default boolean shouldUseLocalSettingsForUnknowns() {
        var server = getServerConfig();
        if (server != null && !server.serverWideSettings.allowIndividualPlayerConfigurations.getValue()) {
            return false;
        }
        var local = getLocalPlayerConfig();
        return local.usePlayerSettingsWhenUndeterminable.getValue();
    }

    /**
     * Sets whether unknown players use the viewer's own settings (Row B, {@code true}) or the global
     * configuration ({@code false}) and optionally persists the change.
     */
    default void setUseOwnSettingsForUnknowns(boolean value, Optional<Boolean> withSave) {
        var local = getLocalPlayerConfig();
        local.usePlayerSettingsWhenUndeterminable.setValue(value);
        if (withSave.isPresent() && withSave.get()) {
            saveLocalPlayerConfig(local);
        }
    }

    /** @return whether the global override configuration is applied to every other player (Row C). */
    default boolean shouldUseGlobalOverrideForAllPlayers() {
        var local = getLocalPlayerConfig();
        return local.useGlobalOverrideForAllPlayers.getValue();
    }

    /**
     * @return whether Armor Hider is fully disabled from the viewer's perspective — either the local kill
     *         switch is on, or the server forces Armor Hider off.
     */
    default boolean isArmorHiderGloballyDisabled() {
        var server = getServerConfig();
        if (server != null && server.serverWideSettings.forceArmorHiderOff.getValue()) {
            return true;
        }
        var local = getLocalPlayerConfig();
        return local.disableArmorHider.getValue();
    }

    /**
     * Whether combat detection should be applied to the given config.
     *
     * <p>Since NOT using combat detection is a potential competitive advantage, the config is probed ahead of
     * the server — the only exception to server settings taking precedence over an individual configuration.
     *
     * @param config the config being rendered.
     * @return {@code true} if combat detection applies (either the config enables it, or the server does).
     */
    default boolean shouldApplyCombatDetectionTo(PlayerConfig config) {
        if (config.enableCombatDetection.getValue()) {
            return true;
        }
        var serverConfig = getServerConfig();
        return serverConfig != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();
    }

    /**
     * Resolves which {@link PlayerConfig} to render the named player with. Precedence (highest first), for a
     * non-local player when other-player config is allowed and the server is not forcing off:
     * <ol>
     *   <li>the local player → the viewer's own live config;</li>
     *   <li>an individual per-player override for this name (only on a server that allows it);</li>
     *   <li>the global override, if it is set to apply to <i>all</i> other players (Row C);</li>
     *   <li>the config the server broadcast for this (modded) player;</li>
     *   <li>otherwise the player is "unknown" → {@link #resolveUnknownPlayerConfig(String, UUID)} (Row B).</li>
     * </ol>
     * When a mod server disallows individual configs, the client controls are inert and the server-broadcast
     * config (or vanilla defaults) is used. The server-wide force-off is applied separately at render time.
     *
     * @param playerName the player's name, or {@code null} if the player could not be identified.
     * @return the configuration to render that player with (never {@code null}).
     */
    PlayerConfig resolveConfig(@Nullable String playerName);

    /**
     * Resolves the config for an unknown (non-mod) player per Row B: the viewer's own settings, or the global
     * override configuration.
     */
    @ApiStatus.Internal
    PlayerConfig resolveUnknownPlayerConfig(String playerName, UUID playerId);
}
