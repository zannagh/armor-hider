//? if <= 1.21.1 {
/*package de.zannagh.armorhider.rendering;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;

public class PlayerPreviewWidget extends AbstractWidget {

    public PlayerPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        LocalPlayer player = Minecraft.getInstance().player;
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
        int borderColor = 0xFFC0C0C0; // Light gray color (1.20.x compatibility)
        context.fill(panelLeft, panelTop, panelRight, panelTop + 1, borderColor); // Top
        context.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, borderColor); // Bottom
        context.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, borderColor); // Left
        context.fill(panelRight - 1, panelTop, panelRight, panelBottom, borderColor); // Right

        drawEntity(
                context,
                previewX,
                previewY - 15,
                (int) Math.round(previewSize * 0.35),
                (float) (previewX - mouseX),
                (float) (previewY - margin - mouseY),
                player
        );
    }

    // Based on InventoryScreen.renderEntityInInventory at 1.20.x
    public static void drawEntity(GuiGraphics context, int x, int y, int size, float mouseX, float mouseY, LivingEntity entity) {
        float f = (float)Math.atan(mouseX / 40.0F);
        float g = (float)Math.atan(mouseY / 40.0F);
        Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf quaternionf2 = new Quaternionf().rotateX(g * 20.0F * (float) (Math.PI / 180.0));
        quaternionf.mul(quaternionf2);
        float h = entity.yBodyRot;
        float i = entity.getYRot();
        float j = entity.getXRot();
        float k = entity.yHeadRotO;
        float l = entity.yHeadRot;
        entity.yBodyRot = 180.0F + f * 20.0F;
        entity.setYRot(180.0F + f * 40.0F);
        entity.setXRot(-g * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        drawEntity(context, x, y, size, quaternionf, quaternionf2, entity);
        entity.yBodyRot = h;
        entity.setYRot(i);
        entity.setXRot(j);
        entity.yHeadRotO = k;
        entity.yHeadRot = l;
    }

    // Based on InventoryScreen.renderEntityInInventory at 1.20.x
    public static void drawEntity(GuiGraphics context, int x, int y, int size, Quaternionf quaternionf, @Nullable Quaternionf quaternionf2, LivingEntity entity) {
        context.pose().pushPose();
        context.pose().translate(x, y, 150.0); // Higher z-value to prevent clipping through background
        //?if >= 1.21
        context.pose().mulPose(new Matrix4f().scaling((float)size, (float)size, (float)(-size)));
        //?if < 1.21
        //context.pose().mulPoseMatrix(new Matrix4f().scaling((float)size, (float)size, (float)(-size)));
        context.pose().mulPose(quaternionf);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (quaternionf2 != null) {
            quaternionf2.conjugate();
            entityRenderDispatcher.overrideCameraOrientation(quaternionf2);
        }

        // Disable depth testing so the entity always renders on top of the background
        RenderSystem.disableDepthTest();
        entityRenderDispatcher.setRenderShadow(false);
        entityRenderDispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F, context.pose(), context.bufferSource(), 15728880);
        context.flush();
        entityRenderDispatcher.setRenderShadow(true);
        RenderSystem.enableDepthTest();
        context.pose().popPose();
        Lighting.setupFor3DItems();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // No narration needed
    }
}
*///?}
