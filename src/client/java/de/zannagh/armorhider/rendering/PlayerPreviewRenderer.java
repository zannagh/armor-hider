package de.zannagh.armorhider.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.util.Colors;

import java.awt.*;

public class PlayerPreviewRenderer {
    private static final int armorHiderSegmentRow = 5;
    
    public static ClickableWidget simpleOptionToWidget(SimpleOption<?> simpleOption, GameOptions options, OptionListWidget body){
        int width = MinecraftClient.getInstance().player != null ? body.getRowWidth() / 2 : body.getRowWidth();
        return simpleOption.createWidget(options, body.getRowLeft(), body.getYOfNextEntry(), width);
    }
    
    public static void renderPlayerPreview(DrawContext context, OptionListWidget body, int mouseX, int mouseY){
        if (!(MinecraftClient.getInstance().player instanceof ClientPlayerEntity player)) {
            return;
        }
        
        int margin = 20;

        int rightHalfWidth = body.getRowWidth() / 2;
        int rightHalfStart = body.getRowLeft() + rightHalfWidth;

        int previewSize = rightHalfWidth - margin * 2;
        int previewX = rightHalfStart + rightHalfWidth / 2; // Center X of right half
        int previewY = body.getRowTop(armorHiderSegmentRow) + 2 + previewSize;

        int panelLeft = previewX - previewSize / 2 - 10;
        int panelTop = previewY - previewSize;
        int panelRight = previewX + previewSize / 2 + 10;
        int panelBottom = previewY + 20;

        int bodyTop = body.getY();
        int bodyBottom = body.getBottom();

        if (panelBottom < bodyTop || panelTop > bodyBottom) {
            return;
        }

        context.enableScissor(
                body.getX(),
                bodyTop,
                body.getX() + body.getWidth(),
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
