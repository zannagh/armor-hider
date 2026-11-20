package de.zannagh.armorhider.client.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.utils.McClientUtils;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;

/**
 * Keeps the integrated server's server-wide settings in line with the client's own settings while playing
 * singleplayer, so a server-side value can never silently override the player's choice (for example the OR of
 * client and server combat detection making armor reappear after it was disabled client-side).
 * <p>
 * Applies only when {@link PlayerConfig#syncServerSettingsInSingleplayer} is on and the client hosts an
 * integrated server that is not opened to LAN. The values travel the regular client-to-server path
 * ({@code SERVER_WIDE_SETTINGS}), so the server persists them into the world's config.
 * <p>
 * Mapping (client setting to server-wide setting):
 * <ul>
 *   <li>{@code enableCombatDetection} to {@code enableCombatDetection}</li>
 *   <li>{@code disableArmorHider} to {@code forceArmorHiderOff}</li>
 *   <li>{@code disableArmorHiderOnInvisibility} to {@code disableArmorHiderOnInvisibilityGlobally}</li>
 *   <li>{@code allowIndividualPlayerConfigurations}: no client counterpart, left as it is on the server</li>
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
        var current = serverConfig.serverWideSettings;
        var desired = new ServerWideSettings(
                local.enableCombatDetection.getValue(),
                local.disableArmorHider.getValue(),
                local.disableArmorHiderOnInvisibility.getValue(),
                current.allowIndividualPlayerConfigurations.getValue());
        if (matches(current, desired)) {
            return;
        }
        ArmorHider.LOGGER.debug("Syncing client settings to the integrated server.");
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.setAndSendServerWideSettings(desired);
    }

    private static boolean matches(ServerWideSettings a, ServerWideSettings b) {
        return a.enableCombatDetection.getValue() == b.enableCombatDetection.getValue()
                && a.forceArmorHiderOff.getValue() == b.forceArmorHiderOff.getValue()
                && a.disableArmorHiderOnInvisibilityGlobally.getValue() == b.disableArmorHiderOnInvisibilityGlobally.getValue()
                && a.allowIndividualPlayerConfigurations.getValue() == b.allowIndividualPlayerConfigurations.getValue();
    }
}
