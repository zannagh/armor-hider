package de.zannagh.armorhider.client.gui.elements;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.client.gui.UiConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if > 1.21.8
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

/**
 * A compound widget that places a primary widget (e.g. slider) at ~80% width
 * and a secondary widget (e.g. toggle button) in the remaining space to its right.
 */
public class CompoundOptionWidget extends AbstractWidget {
    private final AbstractWidget primary;
    private final AbstractWidget secondary;
    @Nullable private final AbstractWidget tertiary;
    @Nullable private final AbstractWidget additional;
    @Nullable private final AbstractWidget quaternary;
    @Nullable private AbstractWidget activeChild;
    private final ElementSpacingOptions spacing;

    public CompoundOptionWidget(AbstractWidget primary, AbstractWidget secondary, @Nullable AbstractWidget tertiary, @Nullable AbstractWidget additional, int width, int height) {
        this(primary, secondary, tertiary, additional, null, width, height);
    }

    public CompoundOptionWidget(AbstractWidget primary, AbstractWidget secondary, @Nullable AbstractWidget tertiary, @Nullable AbstractWidget additional, @Nullable AbstractWidget quaternary, int width, int height) {
        super(0, 0, width, height, Component.empty());
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
        this.additional = additional;
        this.quaternary = quaternary;

        int totalElements = 1 + smallElementCount();
        int sq = UiConstants.SQUARE_BUTTON_WIDTH;
        int g = UiConstants.DEFAULT_BUTTON_SPACING / 2;
        int smalls = smallElementCount();
        int smallGroupWidth = smalls * sq + (smalls - 1) * g;
        int sliderWidth = width - smallGroupWidth - g;

        var groups = new ArrayList<Pair<Integer, Integer>>();
        groups.add(new Pair<>(0, 0));
        groups.add(new Pair<>(1, totalElements - 1));

        this.spacing = new ElementSpacingOptions(width)
                .forEvenElements(sq, totalElements)
                .withGroups(groups)
                .withMinSizesForGroups(new int[]{sq, smallGroupWidth})
                .withSizesForGroups(new int[]{sliderWidth, smallGroupWidth});
    }

    private int smallElementCount() {
        int count = 1; // secondary is always present
        if (additional != null) count++;
        if (tertiary != null) count++;
        if (quaternary != null) count++;
        return count;
    }

    private void updateLayout() {
        primary.setX(this.getX() + spacing.getX(0));
        primary.setY(this.getY());
        primary.setWidth(spacing.getWidth(0));

        int idx = 1;
        secondary.setX(this.getX() + spacing.getX(idx));
        secondary.setY(this.getY());
        secondary.setWidth(spacing.getWidth(idx));
        idx++;

        if (additional != null) {
            additional.setX(this.getX() + spacing.getX(idx));
            additional.setY(this.getY());
            additional.setWidth(spacing.getWidth(idx));
            idx++;
        }

        if (tertiary != null) {
            tertiary.setX(this.getX() + spacing.getX(idx));
            tertiary.setY(this.getY());
            tertiary.setWidth(spacing.getWidth(idx));
            idx++;
        }

        if (quaternary != null) {
            quaternary.setX(this.getX() + spacing.getX(idx));
            quaternary.setY(this.getY());
            quaternary.setWidth(spacing.getWidth(idx));
        }
    }

    public static int getPrimaryWidth(int width) {
        return getPrimaryWidth(width, 3);
    }

    public static int getPrimaryWidth(int width, int smallElements) {
        int sq = UiConstants.SQUARE_BUTTON_WIDTH;
        int g = UiConstants.DEFAULT_BUTTON_SPACING / 2;
        int smallGroupWidth = smallElements * sq + (smallElements - 1) * g;
        return width - smallGroupWidth - g;
    }

    public static int getAdditionalElementWidth(int width) {
        return UiConstants.SQUARE_BUTTON_WIDTH;
    }

    public static int getAdditionalElementWidth(int width, int smallElements) {
        return UiConstants.SQUARE_BUTTON_WIDTH;
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
        if (quaternary != null) {
            quaternary.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
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
        if (quaternary != null) {
            quaternary.render(guiGraphics, mouseX, mouseY, partialTick);
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
        if (quaternary != null) {
            quaternary.render(guiGraphics, mouseX, mouseY, partialTick);
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
        if (quaternary != null && quaternary.mouseClicked(event, doubleClick)) {
            activeChild = quaternary;
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
        if (quaternary != null && quaternary.mouseClicked(d, e, i)) {
            activeChild = quaternary;
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
