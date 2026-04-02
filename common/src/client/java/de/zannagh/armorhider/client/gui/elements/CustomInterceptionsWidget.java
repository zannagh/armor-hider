package de.zannagh.armorhider.client.gui.elements;

//? if >= 26.1-1.pre.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if < 26.1-1.pre.1
//import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

public class CustomInterceptionsWidget extends AbstractWidget {
    private final EquipmentSlot slot;
    public CustomInterceptionsWidget(EquipmentSlot slot,  int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.slot = slot;
    }

    @Override
    //? if >= 26.1-1.pre.1 {
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }
    //?} else if > 1.20.1 {
    /*protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
    *///?} else {
    /*public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
    *///?}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
