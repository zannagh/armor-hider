package de.zannagh.armorhider.client.utils;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.screens.ArmorHiderOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
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
            /*player.sendOverlayMessage(message);
            *///?}
            //? if < 26.1-0.snapshot.11
            player.displayClientMessage(message, true);
        }
    }

    /**
     * Shows a message in the chat HUD (persists in the chat log, unlike the action bar). No-op when there is no
     * local player.
     */
    public static void showChatMessage(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            //? if >= 26.1-0.snapshot.11 {
            /*player.sendSystemMessage(message);
            *///?}
            //? if < 26.1-0.snapshot.11
            player.displayClientMessage(message, false);
        }
    }
    public static @NonNull Boolean isClientConnectedToServer() {
        return Minecraft.getInstance().isLocalServer()
                || Minecraft.getInstance().getCurrentServer() != null
                || (Minecraft.getInstance().getConnection() != null && Minecraft.getInstance().getConnection().getServerData() != null);
    }

    public static Screen getPreferredSettingsScreen(Screen parent, Options options) {
        //? if >= 1.21.9 {
        return ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().showSettingsInSkinCustomization.getValue()
                ? new SkinCustomizationScreen(parent, options)
                : new ArmorHiderOptionsScreen(parent, options);
        //?}
        //? if < 1.21.9
        //return new ArmorHiderOptionsScreen(parent, options);
    }

    public static void openPreferredSettingsScreen(Screen parent, Options options) {
        var minecraft = Minecraft.getInstance();
        minecraft.setScreenAndShow(getPreferredSettingsScreen(parent, options));
    }
}
