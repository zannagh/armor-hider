package de.zannagh.armorhider.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.option.SkinOptionsScreen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Colors;

import java.awt.*;

public class PlayerPreviewRenderer {
    private static final int armorHiderSegmentRow = 5;
    
    public static void renderPlayerPreview(DrawContext context, OptionListWidget body, int mouseX, int mouseY){
        if (!(MinecraftClient.getInstance().player instanceof ClientPlayerEntity player)) {
            return;
        }
        
        if (!(MinecraftClient.getInstance().currentScreen instanceof SkinOptionsScreen)) {
            return;
        }
        
        int rowWidth = RenderUtilities.getRowWidth(body);
        int rowLeft = RenderUtilities.getRowLeft(body);
        int rowTop = RenderUtilities.getRowTop(body, armorHiderSegmentRow);
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

        context.enableScissor(
                bodyX,
                bodyTop,
                bodyX + bodyWidth,
                bodyBottom
        );

        context.fill(panelLeft, panelTop, panelRight, panelBottom, Color.darkGray.darker().getRGB());

        int borderColor = Colors.LIGHT_GRAY;
        context.fill(panelLeft, panelTop, panelRight, panelTop + 1, borderColor); // Top
        context.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, borderColor); // Bottom
        context.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, borderColor); // Left
        context.fill(panelRight - 1, panelTop, panelRight, panelBottom, borderColor); // Right

        InventoryScreen.drawEntity(
                context,
                panelLeft,
                panelTop - margin,
                panelRight,
                panelBottom,
                (int)Math.round(previewSize * 0.5),
                0.25f,
                (float) mouseX,
                (float) mouseY,
                player
        );

        context.disableScissor();
    }
}
