package de.zannagh.armorhider.keybinds;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.configuration.ConfigurationItemBase;
import net.minecraft.network.chat.Component;

import java.awt.event.KeyEvent;

public class ToggleOffKeyMapping extends CustomKeyMapping {
    
    public static String MAPPING_NAME = Component.translatable("key.armorhider.toggle_keybind").getString();
    
    public ToggleOffKeyMapping() {
        //? if > 1.21.8
        super(MAPPING_NAME, KeyEvent.VK_K);
        //? if <= 1.21.8
        // super("Toggle Armor Hider", KeyEvent.VK_K);
    }
    
    @Override
    public void onKeyDown() {
        boolean currentDisable = (boolean) ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.getValue();
        ((ConfigurationItemBase<Boolean>)ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider).setValue(!currentDisable);
    }

    @Override
    public void onKeyUp() { }
}
