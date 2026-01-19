package de.zannagh.armorhider.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.navigation.ScreenDirection;
import org.jetbrains.annotations.Nullable;

public final class RenderUtilities {

    public static int getRowWidth(@Nullable OptionsList body) {
        if (body != null) {
            return body.getRowWidth();
        }
        if (Minecraft.getInstance().screen == null) {
            return Minecraft.getInstance().getWindow().getScreenWidth();
        }

        return Minecraft.getInstance().screen.width;
    }

    public static int getRowLeft(@Nullable OptionsList body) {
        if (body == null && Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.LEFT).left();
        } else if (body != null) {
            return body.getRowLeft();
        }
        return 0;
    }

    public static int getNextY(@Nullable OptionsList body) {
        return body != null
                ? body.getNextY()
                : 0;
    }

    public static int getRowTop(@Nullable OptionsList body, int index) {
        if (body != null) {
            return body.getRowTop(index);
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.DOWN).bottom();
        }
        return 0;
    }

    public static int getBodyTop(@Nullable OptionsList body) {
        if (body != null) {
            return body.getY();
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.DOWN).top();
        }
        return 0;
    }

    public static int getBodyBottom(@Nullable OptionsList body) {
        if (body != null) {
            return body.getBottom();
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.DOWN).bottom();
        }
        return 0;
    }

    public static int getBodyX(@Nullable OptionsList body) {
        if (body != null) {
            return body.getX();
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.LEFT).left();
        }
        return 0;
    }

    public static int getBodyWidth(@Nullable OptionsList body) {
        if (body != null) {
            return body.getWidth();
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.width;
        }
        return 150;
    }
}
