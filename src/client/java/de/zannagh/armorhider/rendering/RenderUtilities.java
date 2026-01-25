package de.zannagh.armorhider.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
//? if >= 1.21.9 {
import net.minecraft.client.gui.navigation.ScreenDirection;
//?}
import org.jetbrains.annotations.Nullable;

public final class RenderUtilities {

    public static int getRowWidth(@Nullable OptionsList body) {
        if (body != null) {
            //? if >= 1.21.9
            return body.getRowWidth();
            //? if < 1.21.9
            //return 310; // Default row width in 1.20.x OptionsList
        }
        if (Minecraft.getInstance().screen == null) {
            return Minecraft.getInstance().getWindow().getScreenWidth();
        }

        return Minecraft.getInstance().screen.width;
    }

    public static int getRowLeft(@Nullable OptionsList body) {
        //? if >= 1.21.9 {
        if (body == null && Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.LEFT).left();
        } else if (body != null) {
            return body.getRowLeft();
        }
        //?}
        //? if < 1.21.9 {
        /*if (body != null && Minecraft.getInstance().screen != null) {
            return (Minecraft.getInstance().screen.width - getRowWidth(body)) / 2;
        }
        *///?}
        return 0;
    }

    public static int getNextY(@Nullable OptionsList body) {
        //? if >= 1.21.9 {
        return body != null
                ? body.getNextY()
                : 0;
        //?}
        //? if < 1.21.9 {
        /*// In 1.20.x, we estimate based on item count and spacing
        if (body != null) {
            return body.children().size() * 25 + 32; // 25px per row + header
        }
        return 0;
        *///?}
    }

    public static int getRowTop(@Nullable OptionsList body, int index) {
        //? if >= 1.21.9 {
        if (body != null) {
            return body.getRowTop(index);
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.DOWN).bottom();
        }
        //?}
        //? if < 1.21.9 {
        /*if (body != null) {
            return 32 + index * 25; // Header + index * row height
        }
        *///?}
        return 0;
    }

    public static int getBodyTop(@Nullable OptionsList body) {
        //? if >= 1.21.9 {
        if (body != null) {
            return body.getY();
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.DOWN).top();
        }
        //?}
        //? if < 1.21.9 {
        /*if (body != null) {
            return 32; // Default top in 1.20.x
        }
        *///?}
        return 0;
    }

    public static int getBodyBottom(@Nullable OptionsList body) {
        //? if >= 1.21.9 {
        if (body != null) {
            return body.getBottom();
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.DOWN).bottom();
        }
        //?}
        //? if < 1.21.9 {
        /*if (body != null && Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.height - 32;
        }
        *///?}
        return 0;
    }

    public static int getBodyX(@Nullable OptionsList body) {
        //? if >= 1.21.9 {
        if (body != null) {
            return body.getX();
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.getRectangle().getBorder(ScreenDirection.LEFT).left();
        }
        //?}
        //? if < 1.21.9 {
        /*if (body != null && Minecraft.getInstance().screen != null) {
            return (Minecraft.getInstance().screen.width - getRowWidth(body)) / 2;
        }
        *///?}
        return 0;
    }

    public static int getBodyWidth(@Nullable OptionsList body) {
        //? if >= 1.21.9 {
        if (body != null) {
            return body.getWidth();
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.width;
        }
        //?}
        //? if < 1.21.9 {
        /*if (body != null) {
            return getRowWidth(body);
        }
        if (Minecraft.getInstance().screen != null) {
            return Minecraft.getInstance().screen.width;
        }
        *///?}
        return 150;
    }
}
