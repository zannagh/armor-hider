package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }
}
