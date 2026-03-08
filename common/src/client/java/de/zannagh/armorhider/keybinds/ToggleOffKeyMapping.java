package de.zannagh.armorhider.keybinds;

import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.awt.event.KeyEvent;

public class ToggleOffKeyMapping extends KeyMapping {
    
    public static String MAPPING_NAME = Component.translatable("key.armorhider.toggle_keybind").getString();
    
    public ToggleOffKeyMapping() {
        //? if > 1.21.8
        super(MAPPING_NAME, KeyEvent.VK_K, Category.MISC);
        //? if <= 1.21.8
        // super("Toggle Armor Hider", KeyEvent.VK_K, "key.categories.misc");
    }

    @Override
    public void setDown(boolean bl) {
        if (!bl) {
            super.setDown(false);
            return;
        }
        boolean currentDisable = (boolean) ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.getValue();
        ((de.zannagh.armorhider.common.configuration.ConfigurationItemBase<Boolean>)ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider).setValue(!currentDisable);
        super.setDown(true);
    }
}
