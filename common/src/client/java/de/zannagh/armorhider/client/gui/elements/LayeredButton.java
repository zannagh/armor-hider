package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

//? if >= 1.21.6
import net.minecraft.client.renderer.RenderPipelines;

//? if < 1.21
//import net.minecraft.client.Minecraft;

public abstract class LayeredButton extends Button {
    protected boolean isEnabled = true;
    @Nullable protected Identifier midLayerSprite() { return null; }
    //? if >= 1.21 {
    protected Identifier spriteBg() {  return this.isHoveredOrFocused() ? Identifier.withDefaultNamespace("widget/button_highlighted") : Identifier.withDefaultNamespace("widget/button"); }
    protected abstract @Nullable Identifier spriteForeground(boolean enabled);
    //?}
    //? if < 1.21 {
    /*@Nullable protected Component statusOverlay() { return null; }
    protected int statusBorderColor() { return 0; }
    *///?}

    //? if >= 1.21
    protected static Identifier modSprite(String name) { return Identifier.fromNamespaceAndPath("armor-hider", name); }
    //? if < 1.21
    //protected static Identifier modSprite(String name) { return new Identifier("armor-hider", name); }

    //? if < 1.21 {
    /*@Nullable private Identifier cachedMidLayerTexture;
    private @Nullable Identifier midLayerTextureId() {
        if (cachedMidLayerTexture == null) {
            var sprite = midLayerSprite();
            if (sprite != null) {
                cachedMidLayerTexture = new Identifier(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png");
            }
        }
        return cachedMidLayerTexture;
    }
    *///?}

    protected final EquipmentSlot slot;

    public LayeredButton(EquipmentSlot slot, int width, int height, Component message, OnPress onPress) {
        super(0, 0, width, height, message, onPress, (discarded) -> MutableComponent.create(message.getContents()));
        this.slot = slot;
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

    public boolean toggle(){
        setEnabled(!isEnabled);
        return isEnabled;
    }

    //? if >= 26.1-1.pre.1 {
    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (spriteForeground(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        if (midLayerSprite() instanceof Identifier sprite) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
    }
    //?}

    //? if < 26.1-1.pre.1 && > 1.21.10 {
    /*
    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (spriteForeground(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        if (midLayerSprite() instanceof Identifier sprite) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
    }
    *///?}

    //? if <= 1.21.10 && >= 1.21.6 {
    /*
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (spriteForeground(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        if (midLayerSprite() instanceof Identifier sprite) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
    }
    *///?}

    //? if <= 1.21.5 && >= 1.21.4 {
    /*
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.RenderType.guiTextured(t), spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (spriteForeground(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.RenderType.guiTextured(t), identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        if (midLayerSprite() instanceof Identifier sprite) {
            guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.RenderType.guiTextured(t), sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
    }
    *///?}

    //? if < 1.21.4 && >= 1.21 {
    /*
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite(spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (spriteForeground(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        if (midLayerSprite() instanceof Identifier sprite) {
            guiGraphics.blitSprite(sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
    }
    *///?}

    //? if < 1.21 {
    /*
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Component savedMsg = this.getMessage();
        super.setMessage(Component.empty());
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        super.setMessage(savedMsg);
        var texture = midLayerTextureId();
        if (texture != null) {
            guiGraphics.blit(texture, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15, 0, 0, 16, 16, 16, 16);
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);
        var overlay = statusOverlay();
        if (overlay != null) {
            var font = Minecraft.getInstance().font;
            guiGraphics.pose().pushPose();
            float cx = this.getX() + this.width / 2f;
            float cy = this.getY() + this.height / 2f;
            guiGraphics.pose().translate(cx, cy, 0);
            guiGraphics.pose().scale(2f, 2f, 1f);
            int hw = font.width(overlay) / 2;
            guiGraphics.drawString(font, overlay, -hw, -4, 0xFFFFFF, true);
            guiGraphics.pose().popPose();
        }
        int borderColor = statusBorderColor();
        if (borderColor != 0) {
            int bx = this.getX();
            int by = this.getY();
            int bw = this.width;
            int bh = this.height;
            int t = 2;
            guiGraphics.fill(bx, by, bx + bw, by + t, borderColor);
            guiGraphics.fill(bx, by + bh - t, bx + bw, by + bh, borderColor);
            guiGraphics.fill(bx, by + t, bx + t, by + bh - t, borderColor);
            guiGraphics.fill(bx + bw - t, by + t, bx + bw, by + bh - t, borderColor);
        }
        guiGraphics.pose().popPose();
    }
    *///?}
}
