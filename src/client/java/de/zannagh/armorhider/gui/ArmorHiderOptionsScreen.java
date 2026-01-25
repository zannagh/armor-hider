// The options screen for 1.21/1.21.1, not used in >= 1.21.9 or 1.20.x

//? if >= 1.21 && < 1.21.9 {

/*package de.zannagh.armorhider.gui;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.NarratedTooltipFactory;
import de.zannagh.armorhider.client.OptionElementFactory;
import de.zannagh.armorhider.rendering.PlayerPreviewWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class ArmorHiderOptionsScreen extends OptionsSubScreen {
    private final Screen parent;
    private boolean hasUsedFallbackWhereServerDidntTranspondSettings = false;

    private boolean settingsChanged;
    private boolean serverSettingsChanged;
    private boolean newServerCombatDetection;

    public ArmorHiderOptionsScreen(Screen parent, Options gameOptions) {
        super(parent, gameOptions, Component.translatable("armorhider.options.regular.title"));
        this.parent = parent;
    }

    @Override
    protected void addOptions() {
        boolean hasPlayer = Minecraft.getInstance().player != null;

        int topMargin = 32;
        int bottomMargin = 32;
        int optionItemHeight = 25;
        int previewMargin = 20;

        
        if (hasPlayer) {
            int listWidth = (this.width * 3) / 5;
            int previewWidth = (this.width * 2) / 5 - previewMargin;
            int previewHeight = this.height - topMargin - bottomMargin - previewMargin * 2;
            int previewX = listWidth + previewMargin / 2;
            int previewY = topMargin + previewMargin;

            PlayerPreviewWidget previewWidget = new PlayerPreviewWidget(
                    previewX,
                    previewY,
                    previewWidth,
                    previewHeight
            );
            addCustomOptionsToOptionListWidget(list, previewWidget);
        } 
        else {
            addCustomOptionsToOptionListWidget(list, null);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    private void addCustomOptionsToOptionListWidget(OptionsList optionListWidget, AbstractWidget playerWidget) {
        OptionElementFactory optionElementFactory = new OptionElementFactory(
            this,
            optionListWidget,
            this.options
        );

        var helmetOption = optionElementFactory.buildDoubleOption(
            "armorhider.helmet.transparency",
            Component.translatable("armorhider.options.helmet.tooltip"),
            Component.translatable("armorhider.options.helmet.tooltip_narration"),
            currentValue -> Component.translatable("armorhider.options.helmet.button_text",
                String.format("%.0f%%", currentValue * 100)),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetOpacity.getValue(),
            this::setHelmetTransparency
        );
        optionElementFactory.addOptionWithWidget(helmetOption, playerWidget);
        
        var skullOrHatOption = optionElementFactory.buildBooleanOption(
            Component.translatable("armorhider.options.helmet_affection.title"),
            Component.translatable("armorhider.options.helmet_affection.tooltip"),
            Component.translatable("armorhider.options.helmet_affection.tooltip_narration"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingHatOrSkull.getValue(),
            this::setOpacityAffectingHatOrSkull
        );
        optionElementFactory.addSimpleOptionAsWidget(skullOrHatOption);

        var chestOption = optionElementFactory.buildDoubleOption(
            "armorhider.chestplate.transparency",
            Component.translatable("armorhider.options.chestplate.tooltip"),
            Component.translatable("armorhider.options.chestplate.tooltip_narration"),
            currentValue -> Component.translatable("armorhider.options.chestplate.button_text",
                String.format("%.0f%%", currentValue * 100)),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestOpacity.getValue(),
            this::setChestTransparency
        );
        optionElementFactory.addSimpleOptionAsWidget(chestOption);

        var elytraOption = optionElementFactory.buildBooleanOption(
            Component.translatable("armorhider.options.elytra_affection.title"),
            Component.translatable("armorhider.options.elytra_affection.tooltip"),
            Component.translatable("armorhider.options.elytra_affection.tooltip_narration"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingElytra.getValue(),
            this::setOpacityAffectingElytra
        );
        optionElementFactory.addSimpleOptionAsWidget(elytraOption);

        var legsOption = optionElementFactory.buildDoubleOption(
            "armorhider.legs.transparency",
            Component.translatable("armorhider.options.leggings.tooltip"),
            Component.translatable("armorhider.options.leggings.tooltip_narration"),
            currentValue -> Component.translatable("armorhider.options.leggings.button_text",
                String.format("%.0f%%", currentValue * 100)),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsOpacity.getValue(),
            this::setLegsTransparency
        );
        optionElementFactory.addSimpleOptionAsWidget(legsOption);

        var bootsOption = optionElementFactory.buildDoubleOption(
            "armorhider.boots.transparency",
            Component.translatable("armorhider.options.boots.tooltip"),
            Component.translatable("armorhider.options.boots.tooltip_narration"),
            currentValue -> Component.translatable("armorhider.options.boots.button_text",
                String.format("%.0f%%", currentValue * 100)),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsOpacity.getValue(),
            this::setBootsTransparency
        );
        optionElementFactory.addSimpleOptionAsWidget(bootsOption);

        OptionInstance<Boolean> enableCombatDetection = optionElementFactory.buildBooleanOption(
            Component.translatable("armorhider.options.combat_detection.title"),
            Component.translatable("armorhider.options.combat_detection.tooltip"),
            Component.translatable("armorhider.options.combat_detection.tooltip_narration"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().enableCombatDetection.getValue(),
            this::setCombatDetection
        );
        optionElementFactory.addSimpleOptionAsWidget(enableCombatDetection);

        // Server-wide combat detection (only for admins)
        if (ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
            boolean serverCombatDetectionValue = serverConfig != null
                    && serverConfig.serverWideSettings != null
                    && serverConfig.serverWideSettings.enableCombatDetection != null
                    ? serverConfig.serverWideSettings.enableCombatDetection.getValue()
                    : getFallbackDefault();
            OptionInstance<Boolean> combatHidingOnServer = optionElementFactory.buildBooleanOption(
                Component.translatable("armorhider.options.combat_detection_server.title"),
                Component.translatable("armorhider.options.combat_detection_server.tooltip"),
                Component.translatable("armorhider.options.combat_detection_server.tooltip_narration"),
                serverCombatDetectionValue,
                this::setServerCombatDetection
            );
            optionElementFactory.addSimpleOptionAsWidget(combatHidingOnServer);
        }

        Component advancedKey = Component.translatable("armorhider.options.regular.title");
        String advancedKeyString;
        if (advancedKey.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatableContents) {
            advancedKeyString = translatableContents.getKey();
        } else {
            advancedKeyString = advancedKey.getString();
        }
        optionListWidget.addBig(
                OptionInstance.createBoolean(
                        advancedKeyString,
                        new NarratedTooltipFactory<>(
                                Component.translatable("armorhider.options.regular.tooltip"), 
                                Component.translatable("armorhider.options.regular.tooltip")),
                        (text, value) -> Component.literal(""),
                        false,
                        value -> {
                            Minecraft.getInstance().setScreen(
                                    new AdvancedArmorHiderSettingsScreen(this, this.options, this.title));
                        }
                ));

        // Advanced settings button is added separately since OptionsList can't hold arbitrary widgets in 1.20.x
    }

    private boolean getFallbackDefault() {
        // Server didn't have the mod, using default value
        hasUsedFallbackWhereServerDidntTranspondSettings = true;
        return true;
    }

    @Override
    public void onClose() {
        if (settingsChanged) {
            ArmorHider.LOGGER.info("Updating current player settings...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
        }
        if (serverSettingsChanged && !hasUsedFallbackWhereServerDidntTranspondSettings) {
            ArmorHider.LOGGER.info("Updating current server settings (if possible)...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.setAndSendServerCombatDetection(newServerCombatDetection);
        }

        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private void setOpacityAffectingHatOrSkull(Boolean value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingHatOrSkull.setValue(value);
        settingsChanged = true;
    }

    private void setOpacityAffectingElytra(Boolean value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingElytra.setValue(value);
        settingsChanged = true;
    }

    private void setHelmetTransparency(double value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetOpacity.setValue(value);
        settingsChanged = true;
    }

    private void setChestTransparency(double value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestOpacity.setValue(value);
        settingsChanged = true;
    }

    private void setLegsTransparency(double value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsOpacity.setValue(value);
        settingsChanged = true;
    }

    private void setBootsTransparency(double value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsOpacity.setValue(value);
        settingsChanged = true;
    }

    private void setCombatDetection(boolean enabled) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().enableCombatDetection.setValue(enabled);
        settingsChanged = true;
    }

    private void setServerCombatDetection(boolean enabled) {
        newServerCombatDetection = enabled;
        serverSettingsChanged = true;
    }
}
*///?}
