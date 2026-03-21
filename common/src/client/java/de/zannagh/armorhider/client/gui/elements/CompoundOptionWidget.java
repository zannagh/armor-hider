package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if > 1.21.8
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

//? if < 26.1-1.pre.1
import net.minecraft.client.gui.GuiGraphics;
//? if >= 26.1-1.pre.1
//import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A compound widget that places a primary widget (e.g. slider) at ~80% width
 * and a secondary widget (e.g. toggle button) in the remaining space to its right.
 */
public class CompoundOptionWidget extends AbstractWidget {
    private final AbstractWidget primary;
    private final AbstractWidget secondary;
    private static final int GAP = 4;

    public CompoundOptionWidget(AbstractWidget primary, AbstractWidget secondary, int width, int height) {
        super(0, 0, width, height, Component.empty());
        this.primary = primary;
        this.secondary = secondary;
    }

    private void updateLayout() {
        int secondaryWidth = this.width - (int) (this.width * 0.6) - GAP;
        int primaryWidth = (int) (this.width * 0.6);

        primary.setX(this.getX());
        primary.setY(this.getY());
        primary.setWidth(primaryWidth);

        secondary.setX(this.getX() + primaryWidth + GAP);
        secondary.setY(this.getY());
        secondary.setWidth(secondaryWidth);
    }

    @Override
    //? if >= 26.1-1.pre.1 {
    /*protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateLayout();
        primary.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        secondary.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }
    *///?}
    //? if > 1.20.1 && < 26.1-1.pre.1 {
    
    protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateLayout();
        primary.render(guiGraphics, mouseX, mouseY, partialTick);
        secondary.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    //?}
    //? if <= 1.20.1 {
    /*public void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateLayout();
        primary.render(guiGraphics, mouseX, mouseY, partialTick);
        secondary.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    *///?}

    @Override
    //? if > 1.21.8
    public boolean mouseClicked(final @NonNull MouseButtonEvent event, final boolean doubleClick) {
    //? if <= 1.21.8
    //public boolean mouseClicked(double d, double e, int i) {
        //? if > 1.21.8 {
        if (primary.mouseClicked(event, doubleClick)) {
            return true;
        }
        return secondary.mouseClicked(event, doubleClick);
        //? }
        //? if <= 1.21.8 {
        
        /*if (primary.mouseClicked(d, e, i)) {
            return true;
        }
        return secondary.mouseClicked(d, e, i);
         
        *///?}
    }

    @Override
    //? if > 1.21.8
    public boolean mouseReleased(final @NonNull MouseButtonEvent event) {
    //? if <= 1.21.8
    //public boolean mouseReleased(double d, double e, int i) {
        //? if > 1.21.8 {
        if (primary.mouseReleased(event)) {
            return true;
        }
        return secondary.mouseReleased(event);
        //?}
        //? if <= 1.21.8 {
        
        /*if (primary.mouseReleased(d, e, i)) {
            return true;
        }
        return secondary.mouseReleased(d, e, i);
         
        *///?}
    }

    @Override
    //? if > 1.21.8
    public boolean mouseDragged(final @NonNull MouseButtonEvent event, final double dx, final double dy) {
    //? if <= 1.21.8
    //public boolean mouseDragged(double d, double e, int i, double f, double g) {
        //? if > 1.21.8 {
        if (primary.mouseDragged(event, dx, dy)) {
            return true;
        }
        return secondary.mouseDragged(event, dx, dy);
        //? }
        //? if <= 1.21.8 {
        
        /*if (primary.mouseDragged(d, e, i, f, g)) {
            return true;
        }
        return secondary.mouseDragged(d, e, i, f, g);
        *///?}
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
        primary.updateNarration(output);
    }
}
