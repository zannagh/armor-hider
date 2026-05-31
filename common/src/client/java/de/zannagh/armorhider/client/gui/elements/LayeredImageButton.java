package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

//? if >= 1.21.6
import net.minecraft.client.renderer.RenderPipelines;

public abstract class LayeredImageButton extends LayeredButton {

    protected abstract @Nullable Identifier spriteForeground(boolean enabled);

    @Nullable protected final EquipmentSlot slot;

    public LayeredImageButton(@Nullable EquipmentSlot slot, int width, int height, Component message, OnPress onPress) {
        super(width, height, message, onPress);
        this.slot = slot;
    }

    @Override
    protected void renderForeground(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        //? if >= 1.21.6 {
        if (spriteForeground(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        //?}
        //? if <= 1.21.5 && >= 1.21.4 {
        /*if (spriteForeground(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.rendertype.RenderType.guiTextured(t), identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        *///?}
        //? if < 1.21.4 && >= 1.21 {
        /*if (spriteForeground(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        *///?}
        //? if < 1.21 {
        /*var fg = spriteForeground(isEnabled);
        if (fg != null) {
            var texture = new Identifier(fg.getNamespace(), "textures/gui/sprites/" + fg.getPath() + ".png");
            guiGraphics.blit(texture, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15, 0, 0, 16, 16, 16, 16);
        }
        *///?}
    }
}
