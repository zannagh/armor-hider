package de.zannagh.armorhider.client.keybinds;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.configuration.ConfigurationItemBase;
import net.minecraft.network.chat.Component;

import java.awt.event.KeyEvent;

public class ToggleOffKeyMapping extends CustomKeyMapping {
    
    public ToggleOffKeyMapping() {
        //? if > 1.21.8
        super("key.armorhider.toggle_keybind", KeyEvent.VK_K);
        //? if <= 1.21.8
         //super("key.armorhider.toggle_keybind", KeyEvent.VK_K);
    }
    
    @Override
    public void onKeyDown() {
        boolean currentDisable = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.getValue();
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.setValue(!currentDisable);
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
    }

    @Override
    public void onKeyUp() { }
}
