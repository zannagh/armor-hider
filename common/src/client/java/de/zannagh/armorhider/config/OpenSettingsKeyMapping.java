package de.zannagh.armorhider.config;

import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;

import java.awt.event.KeyEvent;

public class OpenSettingsKeyMapping extends KeyMapping {
    public OpenSettingsKeyMapping() {
        //? if > 1.21.8
        super("Open Armor Hider Settings", KeyEvent.VK_J, Category.MISC);
        //? if <= 1.21.8
        // super("Open Armor Hider Settings", KeyEvent.VK_J, "key.categories.misc");
    }

    @Override
    public void setDown(boolean bl) {
        if (!bl) {
            super.setDown(false);
            return;
        }
        //? if >= 1.21
        Minecraft.getInstance().setScreenAndShow(new SkinCustomizationScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
        super.setDown(true);
    }
}
