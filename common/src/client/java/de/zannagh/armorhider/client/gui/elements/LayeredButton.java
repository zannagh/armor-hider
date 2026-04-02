package de.zannagh.armorhider.client.gui.elements;

import de.zannagh.armorhider.ArmorHider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationThunk;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

//? if >= 1.21.6
import net.minecraft.client.renderer.RenderPipelines;

import java.util.function.Function;

public abstract class LayeredButton extends Button {
    protected boolean isEnabled = true;
    //? if >= 1.21 {
    protected Identifier spriteBg() {  return this.isHoveredOrFocused() ? Identifier.withDefaultNamespace("widget/button_highlighted") : Identifier.withDefaultNamespace("widget/button"); }
    @Nullable protected ItemStack midLayer() { return null; }
    protected abstract Function<Boolean, @Nullable Identifier> spriteForeground();
    //?}
    
    protected final EquipmentSlot slot;

    public LayeredButton(EquipmentSlot slot, int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
        super(x, y, width, height, message, onPress, createNarration);
        this.slot = slot;
        this.setTooltip(Tooltip.create(message));
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public void setTooltipAndMessage(@NonNull Component message) {
        super.setMessage(message);
        this.setTooltip(Tooltip.create(message));
    }

    public boolean toggle(){
        ArmorHider.LOGGER.info("Toggling button for slot {}", slot);
        ArmorHider.LOGGER.info("Current state: {}", isEnabled);
        isEnabled = !isEnabled;
        ArmorHider.LOGGER.info("New state: {}", isEnabled);
        return isEnabled;
    }

    //? if >= 26.1-1.pre.1 {
    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if ((spriteForeground().apply(isEnabled) instanceof Identifier identifier) && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.width, this.height);
        }
        if (midLayer() instanceof ItemStack itemStack) {
            guiGraphics.item(itemStack, this.getX() + 4, this.getY() + 2);       
        }
    }
    //?}

    //? if < 26.1-1.pre.1 && > 1.21.10 {
    /*
    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if ((spriteForeground().apply(isEnabled) instanceof Identifier identifier) && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.width, this.height);
        }
        if (midLayer() instanceof ItemStack itemStack) {
            guiGraphics.renderItem(itemStack, this.getX() + 4, this.getY() + 2);
        }
    }
    *///?}
    
    //? if <= 1.21.10 && >= 1.21.6 {
    /*
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if ((spriteForeground().apply(isEnabled) instanceof Identifier identifier) && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.width, this.height);
        }
        if (midLayer() instanceof ItemStack itemStack) {
            guiGraphics.renderItem(itemStack, this.getX() + 4, this.getY() + 2);
        }
    }
    *///?}

    //? if <= 1.21.5 && >= 1.21.4 {
    /*
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.RenderType.guiTextured(t), spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if ((spriteForeground().apply(isEnabled) instanceof Identifier identifier) && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite((t) -> net.minecraft.client.renderer.RenderType.guiTextured(t), identifier, this.getX(), this.getY(), this.width, this.height);
        }
        if (midLayer() instanceof ItemStack itemStack) {
            guiGraphics.renderItem(itemStack, this.getX() + 4, this.getY() + 2);  
        }
    }
    *///?}

    //? if < 1.21.4 && >= 1.21 {
    /*
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite(spriteBg(), this.getX(), this.getY(), this.width, this.height);
        if ((spriteForeground().apply(isEnabled) instanceof Identifier identifier) && !identifier.getPath().isEmpty()) {
            guiGraphics.blitSprite(identifier, this.getX(), this.getY(), this.width, this.height);
        }
        if (midLayer() instanceof ItemStack itemStack) {
            guiGraphics.renderItem(itemStack, this.getX() + 4, this.getY() + 2);
        }
    }
    *///?}
}
