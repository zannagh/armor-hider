//? if >= 1.21 {
package de.zannagh.armorhider.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.player.AbstractClientPlayer;

import java.awt.*;

public class PlayerPreviewRenderer {
    private static final int armorHiderSegmentRow = 5;

    public static void renderPlayerPreview(GuiGraphics graphics, OptionsList body, int mouseX, int mouseY) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (!(Minecraft.getInstance().screen instanceof SkinCustomizationScreen)) {
            return;
        }

        int rowWidth = RenderUtilities.getRowWidth(body);
        int rowLeft = RenderUtilities.getRowLeft(body);
        int rowTop = RenderUtilities.getRowTop(body, armorHiderSegmentRow + 1);
        int bodyTop = RenderUtilities.getBodyTop(body);
        int bodyBottom = RenderUtilities.getBodyBottom(body);
        int bodyWidth = RenderUtilities.getBodyWidth(body);
        int bodyX = RenderUtilities.getBodyX(body);

        int margin = 20;

        int rightHalfWidth = rowWidth / 2;
        int rightHalfStart = rowLeft + rightHalfWidth;

        int previewSize = rightHalfWidth - margin * 2;
        int previewX = rightHalfStart + rightHalfWidth / 2; // Center X of right half
        int previewY = rowTop + 2 + previewSize;

        int panelLeft = previewX - previewSize / 2 - 10;
        int panelTop = previewY - previewSize;
        int panelRight = previewX + previewSize / 2 + 10;
        int panelBottom = previewY + 20;


        if (panelBottom < bodyTop || panelTop > bodyBottom) {
            return;
        }

        graphics.enableScissor(
                bodyX,
                bodyTop,
                bodyX + bodyWidth,
                bodyBottom
        );

        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, Color.darkGray.darker().getRGB());

        int borderColor = Color.LIGHT_GRAY.getRGB();
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, borderColor); // Top
        graphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, borderColor); // Bottom
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, borderColor); // Left
        graphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, borderColor); // Right

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                panelLeft,
                panelTop - margin,
                panelRight,
                panelBottom,
                (int) Math.round(previewSize * 0.5),
                0.25f,
                (float) mouseX,
                (float) mouseY,
                player
        );

        graphics.disableScissor();
    }
}
//?}
