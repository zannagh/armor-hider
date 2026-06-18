package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

//? if >= 1.21.6
import net.minecraft.client.renderer.RenderPipelines;

public abstract class LayeredButton extends Button {
    protected boolean isEnabled = true;

    @Nullable protected Identifier midLayerSprite(boolean enabled) { return null; }

    protected Identifier spriteBg() {
        //? if >= 1.21
        return this.isHoveredOrFocused() ? Identifier.withDefaultNamespace("widget/button_highlighted") : Identifier.withDefaultNamespace("widget/button");
        //? if < 1.21
        //return this.isHoveredOrFocused() ? new Identifier("minecraft/textures/gui/widgets/button_highlighted.png") : new Identifier("minecraft/textures/gui/widgets/button.png");
    }

    protected abstract void renderForeground(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a);

    //? if >= 1.21
    protected static Identifier modSprite(String name) { return Identifier.fromNamespaceAndPath("armor-hider", name); }
    //? if < 1.21
    //protected static Identifier modSprite(String name) { return new Identifier("armor-hider", name); }

    public LayeredButton(int width, int height, Component message, OnPress onPress) {
        super(0, 0, width, height, message, onPress, (discarded) -> MutableComponent.create(message.getContents()));
        this.setMessage(message);
        this.setTooltip(Tooltip.create(message));
    }

    protected abstract Component enabledMessage();
    protected abstract Component disabledMessage();

    protected void setEnabled(boolean enabled) {
        isEnabled = enabled;
        if (isEnabled) {
            this.setMessage(enabledMessage());
            this.setTooltip(Tooltip.create(enabledMessage()));
        } else {
            this.setMessage(disabledMessage());
            this.setTooltip(Tooltip.create(disabledMessage()));
        }
    }

    public boolean toggle() {
        setEnabled(!isEnabled);
        return isEnabled;
    }

    //? if >= 26.1-1.pre.1 {
    @Override
    protected void extractContents(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (midLayerSprite(isEnabled) instanceof Identifier sprite) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        renderForeground(guiGraphics, mouseX, mouseY, partialTicks);
    }
    //?}

    //? if < 26.1-1.pre.1 && > 1.21.10 {
    /*@Override
    protected void renderContents(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (midLayerSprite(isEnabled) instanceof Identifier sprite) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        renderForeground(guiGraphics, mouseX, mouseY, partialTicks);
    }
    *///?}

    //? if <= 1.21.10 && >= 1.21.6 {
    /*@Override
    protected void renderWidget(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (midLayerSprite(isEnabled) instanceof Identifier sprite) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        renderForeground(guiGraphics, i, j, f);
    }
    *///?}

    //? if <= 1.21.5 && >= 1.21.4 {
    /*@Override
    protected void renderWidget(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.rendertype.RenderType.guiTextured(t), spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (midLayerSprite(isEnabled) instanceof Identifier sprite) {
            guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.rendertype.RenderType.guiTextured(t), sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        renderForeground(guiGraphics, i, j, f);
    }
    *///?}

    //? if < 1.21.4 && >= 1.21 {
    /*@Override
    protected void renderWidget(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite(spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (midLayerSprite(isEnabled) instanceof Identifier sprite) {
            guiGraphics.blitSprite(sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        renderForeground(guiGraphics, i, j, f);
    }
    *///?}

    //? if < 1.21 {
    /*@Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Component message = this.getMessage();
        super.setMessage(Component.empty());
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        super.setMessage(message);
        var midSprite = midLayerSprite(isEnabled);
        if (midSprite != null) {
            var texture = new Identifier(midSprite.getNamespace(), "textures/gui/sprites/" + midSprite.getPath() + ".png");
            guiGraphics.blit(texture, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15, 0, 0, 16, 16, 16, 16);
        }
        renderForeground(guiGraphics, mouseX, mouseY, partialTicks);
    }
    *///?}
}
