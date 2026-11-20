package de.zannagh.armorhider.client.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.utils.McClientUtils;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import net.minecraft.client.Minecraft;

import java.util.Optional;

/**
 * Keeps the integrated server's server-wide settings in line with the client's own settings while playing
 * singleplayer, so a server-side value can never silently override the player's choice (for example the OR of
 * client and server combat detection making armor reappear after it was disabled client-side).
 * <p>
 * Applies only when {@link PlayerConfig#syncServerSettingsInSingleplayer} is on and the client hosts an
 * integrated server that is not opened to LAN. The values travel the regular client-to-server path
 * ({@code SERVER_WIDE_SETTINGS}), so the server persists them into the world's config.
 * <p>
 * Mapping (client setting to server-wide setting). Both server values are an OR over the client toggle, which is
 * why they are synced: the client toggle would otherwise be ineffective.
 * <ul>
 *   <li>{@code enableCombatDetection} to {@code enableCombatDetection}</li>
 *   <li>{@code disableArmorHiderOnInvisibility} to {@code disableArmorHiderOnInvisibilityGlobally}</li>
 *   <li>{@code forceArmorHiderOff} and {@code allowIndividualPlayerConfigurations}: not synced, left as they are on
 *       the server. Force-off would shadow the session keybind override and would stay set for LAN guests.</li>
 * </ul>
 */
public final class SingleplayerServerSync {

    private SingleplayerServerSync() {
    }

    /** Whether the sync is active for the given local config in the current session. */
    public static boolean applies(PlayerConfig local) {
        return local.syncServerSettingsInSingleplayer.getValue() && McClientUtils.isPrivateSingleplayer();
    }

    /**
     * Same as {@link #pushIfApplicable()}, but hops onto the client thread first. Use from network receive
     * callbacks so the push cannot race with {@code save()} running on the client thread.
     */
    public static void pushOnClientThread() {
        Minecraft.getInstance().execute(SingleplayerServerSync::pushIfApplicable);
    }

    /**
     * Pushes the local config's mapped values to the integrated server if sync applies and they differ from what
     * the server holds. Safe to call at any time: it does nothing until the server config and the admin
     * permission have both arrived, and is idempotent once the values match.
     */
    public static void pushIfApplicable() {
        var local = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig();
        if (local == null || !applies(local)) {
            return;
        }
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        if (serverConfig == null || ArmorHiderClient.permissionLevel < 3) {
            ArmorHider.LOGGER.debug("Singleplayer sync waiting for server config/permission.");
            return;
        }
        var update = computeUpdate(local, serverConfig.serverWideSettings);
        if (update.isPresent()) {
            ArmorHider.LOGGER.debug("Syncing client settings to the integrated server.");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.setAndSendServerWideSettings(update.get());
        }
    }

    /**
     * The decision logic: the settings to send so the server follows the client, or empty if the server already
     * holds the mapped values. Unmapped server values are carried over from {@code current}.
     */
    public static Optional<ServerWideSettings> computeUpdate(PlayerConfig local, ServerWideSettings current) {
        boolean combat = local.enableCombatDetection.getValue();
        boolean invisibility = local.disableArmorHiderOnInvisibility.getValue();
        if (current.enableCombatDetection.getValue() == combat
                && current.disableArmorHiderOnInvisibilityGlobally.getValue() == invisibility) {
            return Optional.empty();
        }
        return Optional.of(new ServerWideSettings(
                combat,
                current.forceArmorHiderOff.getValue(),
                invisibility,
                current.allowIndividualPlayerConfigurations.getValue()));
    }
}
