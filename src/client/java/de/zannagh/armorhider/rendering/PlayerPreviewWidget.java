package de.zannagh.armorhider.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.awt.*;

public class PlayerPreviewWidget extends ClickableWidget {

    public PlayerPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty());
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }

        int margin = 10;
        int previewSize = this.width - margin * 2;
        int previewX = this.getX() + this.width / 2; // Center X
        int previewY = this.getY() + margin + previewSize;

        int panelLeft = previewX - previewSize / 2 - 10;
        int panelTop = previewY - previewSize;
        int panelRight = previewX + previewSize / 2 + 10;
        int panelBottom = previewY + 20;

        // Background
        context.fill(panelLeft, panelTop, panelRight, panelBottom, Color.darkGray.darker().getRGB());

        // Border
        int borderColor = Colors.LIGHT_GRAY;
        context.fill(panelLeft, panelTop, panelRight, panelTop + 1, borderColor); // Top
        context.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, borderColor); // Bottom
        context.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, borderColor); // Left
        context.fill(panelRight - 1, panelTop, panelRight, panelBottom, borderColor); // Right

        // content
        InventoryScreen.drawEntity(
                context,
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
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        // No narration needed 
    }
}
