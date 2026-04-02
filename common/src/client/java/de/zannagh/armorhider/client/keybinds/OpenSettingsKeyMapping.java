package de.zannagh.armorhider.client.keybinds;


import net.minecraft.client.Minecraft;
import de.zannagh.armorhider.client.gui.screens.ArmorHiderOptionsScreen;

import java.awt.event.KeyEvent;

public class OpenSettingsKeyMapping extends CustomKeyMapping {
    
    public OpenSettingsKeyMapping() {
        //? if > 1.21.8 {
        super("key.armorhider.open_settings", KeyEvent.VK_J);
        //?} else 
        //super("key.armorhider.open_settings", KeyEvent.VK_J);
    }

    @Override
    public void onKeyDown() {
        return;
        /*if (Minecraft.getInstance().screen == null) {
        return;
    }
        //? if >= 1.21.9 {
        Minecraft.getInstance().setScreenAndShow(new ArmorHiderOptionsScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
        //? }
        //? if < 1.21.9 && > 1.20.1 {
        //Minecraft.getInstance().setScreen(new ArmorHiderOptionsScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
        //?}
        //? if <= 1.20.1 {
        //Minecraft.getInstance().setScreen(new ArmorHiderOptionsScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
        //?}
        
         */
    }

    @Override
    public void onKeyUp() {
        
    }
}
