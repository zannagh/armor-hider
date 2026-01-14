package de.zannagh.armorhider.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.navigation.ScreenDirection;
import org.jetbrains.annotations.Nullable;

public final class RenderUtilities {
    
    public static int getRowWidth(@Nullable OptionsList body){
        int rowWidth;
        if (body == null) {
            if (Minecraft.getInstance().screen == null) {
                rowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
            }
            else {
                rowWidth = Minecraft.getInstance().screen.width;
            }
        }
        else {
            rowWidth = body.getRowWidth();
        }
        return rowWidth;
    }
    
    public static int getRowLeft(@Nullable OptionsList body) {
        int rowLeft = 0;
        if (body == null && Minecraft.getInstance().screen != null) {
            rowLeft = Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.LEFT).left();
        }
        else if (body != null) {
            rowLeft = body.getRowLeft();
        }
        return rowLeft;
    }
    
    public static int getNextY(@Nullable OptionsList body){
        
        int y = 0;
        if (body != null) {
            y = body.getNextY();
        }
        return y;
    }
    
    public static int getRowTop(@Nullable OptionsList body, int index) {
        int y = 0;
        if (body == null && Minecraft.getInstance().screen != null) {
            y = Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.DOWN).bottom();
        }
        else if (body != null) {
            y = body.getRowTop(index);
        }
        return y;
    }
    
    public static int getBodyTop(@Nullable OptionsList body){
        int y = 0;
        if (body == null && Minecraft.getInstance().screen != null) {
            y = Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.DOWN).top();
        }
        else if (body != null) {
            y = body.getY();
        }
        return y;
    }
    
    public static int getBodyBottom(@Nullable OptionsList body){
        int y = 0;
        if (body == null && Minecraft.getInstance().screen != null) {
            y = Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.DOWN).bottom();
        }
        else if (body != null) {
            y = body.getBottom();
        }
        return y;
    }
    
    public static int getBodyX(@Nullable OptionsList body) {
        int x = 0;
        if (body == null && Minecraft.getInstance().screen != null) {
            x = Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.LEFT).left();
        }
        else if (body != null) {
            x = body.getY();
        }
        return x;
    }
    
    public static int getBodyWidth(@Nullable OptionsList body) {
        int width = 0;
        if (body == null && Minecraft.getInstance().screen != null) {
            width = Minecraft.getInstance().screen.width;
        }
        else if (body != null) {
            width = body.getWidth();
        }
        return width;
    }
}
