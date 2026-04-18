package de.zannagh.armorhider.client.keybinds;


import net.minecraft.client.Minecraft;
import de.zannagh.armorhider.client.ArmorHiderClient;

import java.awt.event.KeyEvent;
import java.nio.channels.NetworkChannel;

public class OpenSettingsKeyMapping extends CustomKeyMapping {
    
    public OpenSettingsKeyMapping() {
        super("key.armorhider.open_settings", KeyEvent.VK_J);
    }

    @Override
    public void onKeyDown() {
        var client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        //? if <= 26.1.2
        var currentScreen = client.screen;
        //? if > 26.1.2
        //var currentScreen = client.gui.screen();
        ArmorHiderClient.openPreferredSettingsScreen(currentScreen, client.options);
    }

    @Override
    public void onKeyUp() { }
}
