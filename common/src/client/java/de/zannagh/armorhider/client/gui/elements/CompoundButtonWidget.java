package de.zannagh.armorhider.client.gui.elements;

import de.zannagh.armorhider.client.gui.UiConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
//? if > 1.21.8
//import net.minecraft.client.input.MouseButtonEvent;

/**
 * A compound widget that evenly spaces up to 8 square buttons within the available row width.
 */
public class CompoundButtonWidget extends AbstractWidget {
    private final AbstractWidget[] buttons;
    @Nullable private AbstractWidget activeChild;
    private final ElementSpacingOptions spacing;

    public CompoundButtonWidget(AbstractWidget[] buttons,
                                int width, int height) {
        super(0, 0, width, height, Component.empty());
        this.buttons = buttons;
        this.spacing = new ElementSpacingOptions(width)
                .forEvenElements(UiConstants.SQUARE_BUTTON_WIDTH, buttons.length);
    }

    public CompoundButtonWidget(AbstractWidget[] buttons,
                                int width, int height,
                                ElementSpacingOptions spacing) {
        super(0, 0, width, height, Component.empty());
        this.buttons = buttons;
        this.spacing = spacing;
    }

    private void updateLayout() {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setX(this.getX() + spacing.getX(i));
            buttons[i].setY(this.getY());
            buttons[i].setWidth(spacing.getWidth(i));
        }
    }

    @Override
    //? if >= 26.1-1.pre.1 {
    /*protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateLayout();
        for (AbstractWidget button : buttons) {
            button.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    *///?}
    //? if > 1.20.1 && < 26.1-1.pre.1 {
    protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateLayout();
        for (AbstractWidget button : buttons) {
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    //?}
    //? if <= 1.20.1 {
    /*public void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateLayout();
        for (AbstractWidget button : buttons) {
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    *///?}

    @Override
    //? if > 1.21.8
    //public boolean mouseClicked(final @NonNull MouseButtonEvent event, final boolean doubleClick) {
    //? if <= 1.21.8
    public boolean mouseClicked(double d, double e, int i) {
        //? if > 1.21.8 {
        /*for (AbstractWidget button : buttons) {
            if (button.mouseClicked(event, doubleClick)) {
                activeChild = button;
                return true;
            }
        }
        return false;
        *///? }
        //? if <= 1.21.8 {

        for (AbstractWidget button : buttons) {
            if (button.mouseClicked(d, e, i)) {
                activeChild = button;
                return true;
            }
        }
        return false;
        //?}
    }

    @Override
    //? if > 1.21.8
    //public boolean mouseReleased(final @NonNull MouseButtonEvent event) {
    //? if <= 1.21.8
    public boolean mouseReleased(double d, double e, int i) {
        //? if > 1.21.8 {
        /*try {
            if (activeChild != null) {
                return activeChild.mouseReleased(event);
            }
            return false;
        } finally {
            activeChild = null;
        }
        *///?}
        //? if <= 1.21.8 {

        try {
            if (activeChild != null) {
                return activeChild.mouseReleased(d, e, i);
            }
            return false;
        } finally {
            activeChild = null;
        }
        //?}
    }

    @Override
    //? if > 1.21.8
    //public boolean mouseDragged(final @NonNull MouseButtonEvent event, final double dx, final double dy) {
    //? if <= 1.21.8
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        //? if > 1.21.8 {
        /*if (activeChild != null) {
            return activeChild.mouseDragged(event, dx, dy);
        }
        return false;
        *///? }
        //? if <= 1.21.8 {

        if (activeChild != null) {
            return activeChild.mouseDragged(d, e, i, f, g);
        }
        return false;
        //?}
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
        if (buttons.length > 0) {
            buttons[0].updateNarration(output);
        }
    }
}
