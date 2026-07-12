package de.zannagh.armorhider.client.utils;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.screens.ArmorHiderOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import org.jspecify.annotations.NonNull;

public final class McClientUtils {
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
