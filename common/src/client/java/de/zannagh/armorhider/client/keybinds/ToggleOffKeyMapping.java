package de.zannagh.armorhider.client.keybinds;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.utils.McClientUtils;
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
        // Flip the transient session override only — never persisted, cleared on disconnect/restart. The old
        // behaviour wrote to disk and survived restarts and config migrations, so an accidental press could
        // silently disable the mod "forever" until the config file was deleted. Feedback on the action bar
        // makes the toggle visible so it can never look like the mod broke.
        boolean nowDisabled = ArmorHiderClient.CLIENT_CONFIG_MANAGER.toggleSessionDisableOverride();
        McClientUtils.showActionBar(Component.translatable(nowDisabled
                ? "armorhider.toggle.disabled"
                : "armorhider.toggle.enabled"));
    }

    @Override
    public void onKeyUp() { }
}
