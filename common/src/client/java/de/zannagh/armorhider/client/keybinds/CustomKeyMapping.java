package de.zannagh.armorhider.client.keybinds;

import net.minecraft.client.KeyMapping;

public abstract class CustomKeyMapping extends KeyMapping {
    
    public CustomKeyMapping(String name, int preferredKey) {
        //? if > 1.21.8
        super(name, preferredKey, Category.MISC);
        //? if <= 1.21.8
         //super(name, preferredKey, "key.categories.misc");
    }
    
    
    @Override
    public void setDown(boolean down) {
        if (!down) {
            onKeyUp();
            super.setDown(false);
            return;
        }
        onKeyDown();
        super.setDown(true);
    }
    
    public abstract void onKeyDown();
    
    public abstract void onKeyUp();
}
