package de.zannagh.armorhider.client.utils;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.screens.ArmorHiderOptionsScreen;
import de.zannagh.armorhider.configuration.SettingsLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class McClientUtils {

    /**
     * Shows a transient message on the action bar (above the hotbar), used for keybind feedback so a toggle is
     * never silent. No-op when there is no local player (e.g. on the title screen).
     */
    public static void showActionBar(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            //? if >= 26.1-0.snapshot.11 {
            player.sendOverlayMessage(message);
            //?}
            //? if < 26.1-0.snapshot.11
            //player.displayClientMessage(message, true);
        }
    }

    /** Shows a system toast (top-right popup) with a title and a message line. */
    public static void showToast(Component title, Component message) {
        var minecraft = Minecraft.getInstance();
        //? if > 26.1.2
        var toasts = minecraft.gui.toastManager();
        //? if > 1.21.1 && <= 26.1.2
        //var toasts = minecraft.getToastManager();
        //? if <= 1.21.1
        //var toasts = minecraft.getToasts();
        //? if >= 1.21
        SystemToast.add(toasts, SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message);
        //? if < 1.21
        //SystemToast.add(toasts, SystemToast.SystemToastIds.PERIODIC_NOTIFICATION, title, message);
    }

    /**
     * Shows a message in the chat HUD (persists in the chat log, unlike the action bar). No-op when there is no
     * local player.
     */
    public static void showChatMessage(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            //? if >= 26.1-0.snapshot.11 {
            player.sendSystemMessage(message);
            //?}
            //? if < 26.1-0.snapshot.11
            //player.displayClientMessage(message, false);
        }
    }
    /**
     * Whether this client hosts an integrated server (singleplayer or an opened-to-LAN world). Replaces the
     * old check that sat behind {@code getCurrentServer() != null}, which is always null for an integrated server.
     */
    public static boolean isIntegratedServerHost() {
        return Minecraft.getInstance().getSingleplayerServer() != null;
    }

    /**
     * Whether this client hosts an integrated server that has NOT been opened to LAN, i.e. nobody else can be
     * connected to it. Turns false as soon as the world is published to LAN.
     */
    public static boolean isPrivateSingleplayer() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        return server != null && !server.isPublished();
    }

    public static @NonNull Boolean isClientConnectedToServer() {
        return Minecraft.getInstance().isLocalServer()
                || Minecraft.getInstance().getCurrentServer() != null
                || (Minecraft.getInstance().getConnection() != null && Minecraft.getInstance().getConnection().getServerData() != null);
    }

    public static Screen getPreferredSettingsScreen(Screen parent, Options options) {
        //? if >= 1.21.9 {
        return ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().settingsScreenLocation.getValue() == SettingsLocation.SKIN_CUSTOMIZATION
                ? new SkinCustomizationScreen(parent, options)
                : new ArmorHiderOptionsScreen(parent, options);
        //?}
        //? if < 1.21.9
        //return new ArmorHiderOptionsScreen(parent, options);
    }

    public static void openPreferredSettingsScreen(Screen parent, Options options) {
        // Backstop for every door into the settings UI. The individual entry points (the options
        // button, the skin-customization panel, ModMenu, the keybind) are suppressed at their own
        // sites in API-only mode; guarding the one place that actually opens a screen means a future
        // entry point cannot reintroduce the UI by forgetting the check.
        if (ArmorHider.isApiOnly()) {
            ArmorHider.LOGGER.debug("Suppressing the settings screen: Armor Hider runs in API-only mode.");
            return;
        }
        var minecraft = Minecraft.getInstance();
        minecraft.setScreenAndShow(getPreferredSettingsScreen(parent, options));
    }
}
