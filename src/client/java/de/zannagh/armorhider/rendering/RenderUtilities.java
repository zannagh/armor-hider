package de.zannagh.armorhider.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import org.jetbrains.annotations.Nullable;

public final class RenderUtilities {
    
    public static int getRowWidth(@Nullable OptionListWidget body){
        int rowWidth;
        if (body == null) {
            if (MinecraftClient.getInstance().currentScreen == null) {
                rowWidth = MinecraftClient.getInstance().getWindow().getWidth();
            }
            else {
                rowWidth = MinecraftClient.getInstance().currentScreen.width;
            }
        }
        else {
            rowWidth = body.getRowWidth();
        }
        return rowWidth;
    }
    
    public static int getRowLeft(@Nullable OptionListWidget body) {
        int rowLeft = 0;
        if (body != null) {
            rowLeft = body.getRowLeft();
        }
        return rowLeft;
    }
    
    public static int getNextY(@Nullable OptionListWidget body){
        
        int y = 0;
        if (body != null && MinecraftClient.getInstance().currentScreen instanceof GameOptionsScreen screen) {
            y = Math.toIntExact(screen.layout.getContentHeight() * (long) body.children().size());
        }
        return y;
    }
    
    public static int getRowTop(@Nullable OptionListWidget body, int index) {
        return ThreePartsLayoutWidget.DEFAULT_HEADER_FOOTER_HEIGHT * index;
    }
    
    public static int getBodyTop(@Nullable OptionListWidget body){
        return getBodyX(body);
    }
    
    public static int getBodyBottom(@Nullable OptionListWidget body){
        int y = 0;
        if (body != null) {
            y = body.getBottom();
        }
        return y;
    }
    
    public static int getBodyX(@Nullable OptionListWidget body) {
        int x = 0;
        if (body != null) {
            x = body.getY();
        }
        return x;
    }
    
    public static int getBodyWidth(@Nullable OptionListWidget body) {
        int width = 0;
        if (body == null && MinecraftClient.getInstance().currentScreen != null) {
            width = MinecraftClient.getInstance().currentScreen.width;
        }
        else if (body != null) {
            width = body.getWidth();
        }
        return width;
    }
}
