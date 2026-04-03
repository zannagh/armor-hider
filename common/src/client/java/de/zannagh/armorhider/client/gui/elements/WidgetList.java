package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
//? if >= 26.1-1.pre.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if < 26.1-1.pre.1
//import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

import java.util.List;
import java.util.function.Consumer;

public class WidgetList extends ContainerObjectSelectionList<WidgetList.WidgetEntry> {

    private final int contentWidth;

    //? if >= 1.21 {
    public WidgetList(Minecraft mc, int width, int height, int y, int itemHeight) {
        super(mc, width, height, y, itemHeight);
        this.contentWidth = width;
    }
    //?} else {
    /*public WidgetList(Minecraft mc, int width, int height, int y, int itemHeight) {
        super(mc, width, height, y, y + height, itemHeight);
        this.contentWidth = width;
    }
    *///?}

    public void addWidget(AbstractWidget widget) {
        addEntry(new WidgetEntry(widget));
    }

    @Override
    public int getRowWidth() {
        return Math.max(0, Math.min(contentWidth - 20, 310));
    }

    //? if < 1.21 {
    /*@Override
    protected int getScrollbarPosition() {
        return (this.width + this.getRowWidth()) / 2 + 4;
    }
    *///?}

    public static class WidgetEntry extends ContainerObjectSelectionList.Entry<WidgetEntry> {
        private final AbstractWidget widget;

        public WidgetEntry(AbstractWidget widget) {
            this.widget = widget;
        }

        //? if >= 26.1-1.pre.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float delta) {
            widget.setX(this.getContentX());
            widget.setY(this.getContentY());
            widget.setWidth(this.getContentWidth());
            widget.extractRenderState(context, mouseX, mouseY, delta);
        }
        //?} else if >= 1.21.9 {
        /*@Override
        public void renderContent(GuiGraphics context, int mouseX, int mouseY, boolean hovered, float delta) {
            widget.setX(this.getContentX());
            widget.setY(this.getContentY());
            widget.setWidth(this.getContentWidth());
            widget.render(context, mouseX, mouseY, delta);
        }
        *///?} else {
        /*@Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
            widget.setX(x);
            widget.setY(y);
            widget.setWidth(entryWidth);
            widget.render(context, mouseX, mouseY, delta);
        }
        *///?}

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(widget);
        }

        //? if >= 1.21.9 {
        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer) {
            consumer.accept(widget);
        }
        //?}
    }
}
