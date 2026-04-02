package de.zannagh.armorhider.client.gui.screens;

import net.minecraft.client.gui.components.AbstractWidget;

public interface InjectableScreen {
    void addWidget(AbstractWidget widget);
    
    void removeWidget(AbstractWidget widget);
}
