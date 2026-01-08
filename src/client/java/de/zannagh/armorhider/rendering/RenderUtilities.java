package de.zannagh.armorhider.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.OptionListWidget;
import org.jetbrains.annotations.Nullable;

public final class RenderUtilities {

    // 1.20.1 compatibility: hardcoded layout constants
    private static final int OPTION_HEIGHT = 25;
    private static final int HEADER_FOOTER_HEIGHT = 32;
    
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
        // 1.20.1 compatibility: calculate Y position based on number of children
        int y = 0;
        if (body != null) {
            y = OPTION_HEIGHT * body.children().size();
        }
        return y;
    }
    
    public static int getRowTop(@Nullable OptionListWidget body, int index) {
        // 1.20.1 compatibility: use hardcoded header/footer height
        return HEADER_FOOTER_HEIGHT * index;
    }
    
    public static int getBodyTop(@Nullable OptionListWidget body){
        return getBodyX(body);
    }
    
    public static int getBodyBottom(@Nullable OptionListWidget body){
        // 1.20.1 compatibility: OptionListWidget doesn't have getBottom() method
        // Return screen height as fallback
        if (MinecraftClient.getInstance().currentScreen != null) {
            return MinecraftClient.getInstance().currentScreen.height;
        }
        return 0;
    }

    public static int getBodyX(@Nullable OptionListWidget body) {
        // 1.20.1 compatibility: OptionListWidget doesn't have getY() method
        // Return header height as typical starting position
        return HEADER_FOOTER_HEIGHT;
    }

    public static int getBodyWidth(@Nullable OptionListWidget body) {
        // 1.20.1 compatibility: OptionListWidget doesn't have getWidth() method
        if (MinecraftClient.getInstance().currentScreen != null) {
            return MinecraftClient.getInstance().currentScreen.width;
        }
        return 0;
    }
}
