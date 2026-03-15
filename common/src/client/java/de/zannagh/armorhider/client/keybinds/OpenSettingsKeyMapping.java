package de.zannagh.armorhider.client.keybinds;

import net.minecraft.client.Minecraft;
//? if > 1.20.1
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
//? if <= 1.20.1
 //import de.zannagh.armorhider.client.gui.screens.OldArmorHiderOptionsScreen;
import net.minecraft.network.chat.Component;

import java.awt.event.KeyEvent;

public class OpenSettingsKeyMapping extends CustomKeyMapping {
    
    public OpenSettingsKeyMapping() {
        //? if > 1.21.8
        super("key.armorhider.open_settings", KeyEvent.VK_J);
        //? if <= 1.21.8
         //super("key.armorhider.open_settings", KeyEvent.VK_J);
    }

    @Override
    public void onKeyDown() {
        //? if >= 1.21.9 {
        assert Minecraft.getInstance().screen != null;
        Minecraft.getInstance().setScreenAndShow(new SkinCustomizationScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
        //? }
        //? if < 1.21.9 && > 1.20.1 {
        /*
        assert Minecraft.getInstance().screen != null;
        Minecraft.getInstance().setScreen(new SkinCustomizationScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
         */
        //?}
        //? if <= 1.20.1 {
        
        /*assert Minecraft.getInstance().screen != null;
        Minecraft.getInstance().setScreen(new OldArmorHiderOptionsScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
         
        *///?}
    }

    @Override
    public void onKeyUp() { }
}
