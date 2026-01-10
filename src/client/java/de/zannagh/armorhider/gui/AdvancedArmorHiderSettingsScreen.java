package de.zannagh.armorhider.gui;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.OptionElementFactory;
import de.zannagh.armorhider.config.ClientConfigManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.server.command.ReturnCommand;
import net.minecraft.text.Text;

import java.util.Optional;

public class AdvancedArmorHiderSettingsScreen extends GameOptionsScreen {
    public AdvancedArmorHiderSettingsScreen(Screen parent, GameOptions gameOptions, Text title) {
        super(parent, gameOptions, title);
    }

    
    @Override
    protected void addOptions() {
        OptionElementFactory optionElementFactory = new OptionElementFactory(this, body, gameOptions);
        var settingsToUse = optionElementFactory.buildBooleanOption(
                Text.literal("Settings to use when not determinable"), 
                Text.literal("Whether to use your own settings or the default settings when a player's settings cannot be determined."),
                null,
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().usePlayerSettingsWhenUndeterminable.getValue(),
                (value) -> {
                    ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().usePlayerSettingsWhenUndeterminable.setValue(value);
                    ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
                }
        );
        
        optionElementFactory.addSimpleOptionAsWidget(settingsToUse);
    }
}
