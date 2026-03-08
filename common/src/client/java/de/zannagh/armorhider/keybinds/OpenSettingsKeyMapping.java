package de.zannagh.armorhider.keybinds;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//? if > 1.20.1
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
//? if <= 1.20.1
// import de.zannagh.armorhider.gui.OldArmorHiderOptionsScreen;
import net.minecraft.network.chat.Component;

import java.awt.event.KeyEvent;

public class OpenSettingsKeyMapping extends KeyMapping {
    
    public static String MAPPING_NAME = Component.translatable("key.armorhider.open_settings").getString();
    
    public OpenSettingsKeyMapping() {
        //? if > 1.21.8
        super(MAPPING_NAME, KeyEvent.VK_J, Category.MISC);
        //? if <= 1.21.8
        // super(MAPPING_NAME, KeyEvent.VK_J, "key.categories.misc");
    }

    @Override
    public void setDown(boolean bl) {
        if (!bl) {
            super.setDown(false);
            return;
        }
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
        /*
        assert Minecraft.getInstance().screen != null;
        Minecraft.getInstance().setScreen(new OldArmorHiderOptionsScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
         */
        //?}
        super.setDown(true);
    }
}
