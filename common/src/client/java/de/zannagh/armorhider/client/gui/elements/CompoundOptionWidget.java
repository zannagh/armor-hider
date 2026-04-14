package de.zannagh.armorhider.client.gui.elements;

import de.zannagh.armorhider.client.gui.UiConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if > 1.21.8
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

/**
 * A compound widget that places a primary widget (e.g. slider) at ~80% width
 * and a secondary widget (e.g. toggle button) in the remaining space to its right.
 */
public class CompoundOptionWidget extends AbstractWidget {
    private final AbstractWidget primary;
    private final AbstractWidget secondary;
    @Nullable private final AbstractWidget tertiary;
    @Nullable private final AbstractWidget additional;
    @Nullable private AbstractWidget activeChild;
    private static final int GAP = UiConstants.DEFAULT_BUTTON_SPACING / 2;

    public CompoundOptionWidget(AbstractWidget primary, AbstractWidget secondary, @Nullable AbstractWidget tertiary, @Nullable AbstractWidget additional, int width, int height) {
        super(0, 0, width, height, Component.empty());
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
        this.additional = additional;
    }

    private void updateLayout() {
        int additionalElementWidth = getAdditionalElementWidth(this.width);
        int primaryWidth = getPrimaryWidth(this.width);
        
        int firstElementX = this.getX() + this.width - additionalElementWidth * 3 - GAP * 2;
        int secondElementX = this.getX() + this.width - additionalElementWidth * 2 - GAP;
        int thirdElementX = this.getX() + this.width - additionalElementWidth;
        
        primary.setX(this.getX());
        primary.setY(this.getY());
        primary.setWidth(primaryWidth);

        secondary.setX(firstElementX);
        secondary.setY(this.getY());
        secondary.setWidth(additionalElementWidth);
        
        if (tertiary != null) {
            tertiary.setY(this.getY());
            tertiary.setWidth(additionalElementWidth);
            tertiary.setX(thirdElementX);
        }
        
        if (additional != null) {
            additional.setX(secondElementX);
            additional.setY(this.getY());
            additional.setWidth(additionalElementWidth);
        }
    }
    
    private static final double SMALL_ELEMENT_WIDTH_PERCENT = 0.1;
    
    public static int getPrimaryWidth(int width) {
        int smallElements = 3; // Currently 3 buttons next to the sliders.
        return (int) (width * (1 - smallElements * SMALL_ELEMENT_WIDTH_PERCENT)) - GAP;
    }
    
    public static int getAdditionalElementWidth(int width) {
        return (int) (width * SMALL_ELEMENT_WIDTH_PERCENT) - GAP;
    }

    @Override
    //? if >= 26.1-1.pre.1 {
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateLayout();
        primary.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        secondary.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        if (tertiary != null) {
            tertiary.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        if (additional != null) {
            additional.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    //?}
    //? if > 1.20.1 && < 26.1-1.pre.1 {
    /*protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateLayout();
        primary.render(guiGraphics, mouseX, mouseY, partialTick);
        secondary.render(guiGraphics, mouseX, mouseY, partialTick);
        if (tertiary != null) {
            tertiary.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        if (additional != null) {
            additional.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    *///?}
    //? if <= 1.20.1 {
    /*public void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateLayout();
        primary.render(guiGraphics, mouseX, mouseY, partialTick);
        secondary.render(guiGraphics, mouseX, mouseY, partialTick);
        
        if (tertiary != null) {
            tertiary.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        if (additional != null) {
            additional.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    *///?}

    @Override
    //? if > 1.21.8
    public boolean mouseClicked(final @NonNull MouseButtonEvent event, final boolean doubleClick) {
    //? if <= 1.21.8
    //public boolean mouseClicked(double d, double e, int i) {
        //? if > 1.21.8 {
        if (primary.mouseClicked(event, doubleClick)) {
            activeChild = primary;
            return true;
        }
        if (secondary.mouseClicked(event, doubleClick)) {
            activeChild = secondary;
            return true;
        }
        if (additional != null && additional.mouseClicked(event, doubleClick)) {
            activeChild = additional;
            return true;
        }
        if (tertiary != null && tertiary.mouseClicked(event, doubleClick)) {
            activeChild = tertiary;
            return true;
        }
        return false;
        //? }
        //? if <= 1.21.8 {

        /*if (primary.mouseClicked(d, e, i)) {
            activeChild = primary;
            return true;
        }
        if (secondary.mouseClicked(d, e, i)) {
            activeChild = secondary;
            return true;
        }
        if (additional != null && additional.mouseClicked(d, e, i)) {
            activeChild = additional;
            return true;
        }
        if (tertiary != null && tertiary.mouseClicked(d, e, i)) {
            activeChild = tertiary;
            return true;
        }
        return false;
        *///?}
    }

    @Override
    //? if > 1.21.8
    public boolean mouseReleased(final @NonNull MouseButtonEvent event) {
    //? if <= 1.21.8
    //public boolean mouseReleased(double d, double e, int i) {
        //? if > 1.21.8 {
        try {
            if (activeChild != null) {
                return activeChild.mouseReleased(event);
            }
            return false;
        } finally {
            activeChild = null;
        }
        //?}
        //? if <= 1.21.8 {

        /*try {
            if (activeChild != null) {
                return activeChild.mouseReleased(d, e, i);
            }
            return false;
        } finally {
            activeChild = null;
        }
        *///?}
    }

    @Override
    //? if > 1.21.8
    public boolean mouseDragged(final @NonNull MouseButtonEvent event, final double dx, final double dy) {
    //? if <= 1.21.8
    //public boolean mouseDragged(double d, double e, int i, double f, double g) {
        //? if > 1.21.8 {
        if (activeChild != null) {
            return activeChild.mouseDragged(event, dx, dy);
        }
        return false;
        //? }
        //? if <= 1.21.8 {

        /*if (activeChild != null) {
            return activeChild.mouseDragged(d, e, i, f, g);
        }
        return false;
        *///?}
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
        primary.updateNarration(output);
    }
}
