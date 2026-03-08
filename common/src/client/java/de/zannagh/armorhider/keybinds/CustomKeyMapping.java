package de.zannagh.armorhider.keybinds;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.predicates.DataComponentPredicate;

import javax.naming.InterruptedNamingException;
import java.util.Arrays;

public abstract class CustomKeyMapping extends KeyMapping {
    
    private static final int A_KEY_CODE = 65;
    
    public CustomKeyMapping(String name, int preferredKey) {
        //? if > 1.21.8
        super(name, preferredKey, Category.MISC);
        //? if <= 1.21.8
        // super(name, preferredKey, "key.categories.misc");
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
