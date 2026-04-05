package de.zannagh.armorhider.client.gui.screens;

import de.zannagh.armorhider.client.gui.elements.OptionElementFactory;
import de.zannagh.armorhider.client.gui.elements.WidgetList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class ArmorHiderConfigurationScreen extends Screen {
    
    @Nullable protected final Screen parent;
   
    protected boolean settingsChanged = false;
    
    protected WidgetList widgetList;
    
    protected int rowWidth = this.width;
    
    protected OptionElementFactory factory;
    
    protected Options gameOptions;
    
    protected ArmorHiderConfigurationScreen(@Nullable Screen parent, Options gameOptions, Component title) {
        super(title);
        this.parent = parent;
        this.gameOptions = gameOptions;
    }
    
    protected final int topMargin = 32;
    protected final int bottomMargin = 32;
    protected final int itemHeight = 25;
    protected final int previewMargin = 20;

    /**
     * Initializes the screen by calling the addOptions method and using the previously configured widget list (adding it to the renderables).
     */
    @Override
    protected void init(){
        addOptions();
        addRenderableWidget(widgetList);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    protected abstract void addOptions();

    /**
     * Initializes the widget list and the element factory.
     * @param width The width of the widget list.
     */
    protected void initWidgetList(int width) {
        widgetList = new WidgetList(this.minecraft, width, this.height - topMargin - bottomMargin, topMargin, itemHeight);
        rowWidth = widgetList.getRowWidth();
        factory = new OptionElementFactory(widgetList::addWidget, gameOptions, rowWidth);
    }
    
    protected boolean isPlayerInGame() {
        return this.minecraft.player != null;
    }
    
    protected abstract void saveSettingsOnClose();
    
    @Override
    public void onClose() {
        if (settingsChanged) {
            saveSettingsOnClose();
        }
        this.minecraft.setScreen(parent);
    }
    
    protected <T> void setSetting(T value, Consumer<T> setter){
        setter.accept(value);
        settingsChanged = true;
    }

    public void addWidget(AbstractWidget widget){
        addRenderableWidget(widget);
    }
    
    void removeWidget(AbstractWidget widget) {
        removeWidget((GuiEventListener) widget);
    }
}
