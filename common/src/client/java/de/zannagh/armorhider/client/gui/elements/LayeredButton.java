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

    /**
     * Returns the sprite for the middle layer of the button.
     * @param enabled whether the button is enabled
     * @return the sprite for the middle layer, or null if no sprite is available
     */
    @Nullable protected Identifier midLayerSprite(boolean enabled) { return null; }

    /**
     * Returns the background sprite for the button.
     * @return the background sprite for the button.
     */
    protected Identifier spriteBg() {  
        //? if >= 1.21
        return this.isHoveredOrFocused() ? Identifier.withDefaultNamespace("widget/button_highlighted") : Identifier.withDefaultNamespace("widget/button"); 
        //? if < 1.21
        //return this.isHoveredOrFocused() ? new Identifier("minecraft/textures/gui/widgets/button_highlighted.png") : new Identifier("minecraft/textures/gui/widgets/button.png");
    }

    /**
     * Returns the foreground sprite for the button.
     * @param enabled whether the button is enabled
     * @return the foreground sprite for the button, or null if no sprite is available
     */
    protected abstract @Nullable Identifier spriteForeground(boolean enabled);

    //? if >= 1.21
    protected static Identifier modSprite(String name) { return Identifier.fromNamespaceAndPath("armor-hider", name); }
    //? if < 1.21
    //protected static Identifier modSprite(String name) { return new Identifier("armor-hider", name); }

    //? if < 1.21 {
    /*
    private @Nullable Identifier getTextureId(@Nullable Identifier id) {
        if (id == null) { return null; }
        return new Identifier(id.getNamespace(), "textures/gui/sprites/" + id.getPath() + ".png");
    }
    *///?}

    @Nullable protected final EquipmentSlot slot;

    public LayeredButton(@Nullable EquipmentSlot slot, int width, int height, Component message, OnPress onPress) {
        super(0, 0, width, height, message, onPress, (discarded) -> MutableComponent.create(message.getContents()));
        this.slot = slot;
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

    public boolean toggle(){
        setEnabled(!isEnabled);
        return isEnabled;
    }

    //? if >= 26.1-1.pre.1 {
    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (midLayerSprite(isEnabled) instanceof Identifier sprite) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        if (spriteForeground(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
    }
    //?}

    //? if < 26.1-1.pre.1 && > 1.21.10 {
    /*
    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (midLayerSprite(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        if (spriteForeground(isEnabled) instanceof Identifier sprite) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
    }
    *///?}

    //? if <= 1.21.10 && >= 1.21.6 {
    
    /*@Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (midLayerSprite(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        if (spriteForeground(isEnabled) instanceof Identifier sprite) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
    }
    *///?}

    //? if <= 1.21.5 && >= 1.21.4 {
    
    /*@Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.rendertype.RenderType.guiTextured(t), spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (midLayerSprite(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.rendertype.RenderType.guiTextured(t), identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        if (spriteForeground(isEnabled) instanceof Identifier sprite) {
            guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.rendertype.RenderType.guiTextured(t), sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
    }
    *///?}

    //? if < 1.21.4 && >= 1.21 {
    
    /*@Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite(spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if (midLayerSprite(isEnabled) instanceof Identifier identifier && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(identifier, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
        if (spriteForeground(isEnabled) instanceof Identifier sprite) {
            guiGraphics.blitSprite(sprite, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15);
        }
    }
    *///?}

    //? if < 1.21 {
    /*
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Component message = this.getMessage();
        super.setMessage(Component.empty());
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        super.setMessage(message);
        var texture = getTextureId(midLayerSprite(isEnabled));
        if (texture != null) {
            guiGraphics.blit(texture, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15, 0, 0, 16, 16, 16, 16);
        }
        var foreGroundTexture = getTextureId(spriteForeground(isEnabled));
        if (foreGroundTexture != null) {
            guiGraphics.blit(foreGroundTexture, this.getX() + (this.width - 15) / 2, this.getY() + (this.height - 15) / 2, 15, 15, 0, 0, 16, 16, 16, 16);
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);
        guiGraphics.pose().popPose();
    }
    *///?}
}
