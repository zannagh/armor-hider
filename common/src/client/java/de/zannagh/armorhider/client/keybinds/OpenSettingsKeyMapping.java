package de.zannagh.armorhider.client.keybinds;


import de.zannagh.armorhider.client.utils.McClientUtils;
import net.minecraft.client.Minecraft;
import de.zannagh.armorhider.client.ArmorHiderClient;

import java.awt.event.KeyEvent;
import java.nio.channels.NetworkChannel;

public class OpenSettingsKeyMapping extends CustomKeyMapping {
    
    public OpenSettingsKeyMapping() {
        super("key.armorhider.open_settings", KeyEvent.VK_J);
    }

    @Override
    protected void armorHider$onActivated() {
        var client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        // Presses only register while no screen is open, so this is normally null and closing the
        // settings screen returns to the world. Read it anyway so the parent stays correct if a
        // future path ever triggers this with a screen already up.
        //? if <= 26.1.2
        //var currentScreen = client.screen;
        //? if > 26.1.2
        var currentScreen = client.gui.screen();
        McClientUtils.openPreferredSettingsScreen(currentScreen, client.options);
    }
}
