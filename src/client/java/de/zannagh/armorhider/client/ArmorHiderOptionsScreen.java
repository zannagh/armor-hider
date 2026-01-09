// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.rendering.PlayerPreviewWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ArmorHiderOptionsScreen extends GameOptionsScreen {
    private final Screen parent;
    private boolean hasUsedFallbackWhereServerDidntTranspondSettings = false;

    private boolean settingsChanged;
    private boolean serverSettingsChanged;
    private boolean newServerCombatDetection;

    public ArmorHiderOptionsScreen(Screen parent, GameOptions gameOptions) {
        super(parent, gameOptions, Text.translatable("armorhider.options.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boolean hasPlayer = MinecraftClient.getInstance().player != null;

        int topMargin = 32;
        int bottomMargin = 32;
        int optionItemHeight = 25;
        int previewMargin = 20;

        OptionListWidget optionListWidget;

        if (hasPlayer) {
            int listWidth = (this.width * 3) / 5;

            optionListWidget = new OptionListWidget(
                this.client,
                listWidth,
                this.height,
                topMargin,
                this.height - bottomMargin,
                    optionItemHeight
            );

            addCustomOptionsToOptionListWidget(optionListWidget);
            this.addDrawableChild(optionListWidget);

            int previewWidth = (this.width * 2) / 5 - previewMargin; 
            int previewHeight = this.height - topMargin - bottomMargin - previewMargin*2;
            int previewX = listWidth + previewMargin / 2;
            int previewY = topMargin + previewMargin;

            PlayerPreviewWidget previewWidget = new PlayerPreviewWidget(
                previewX,
                previewY,
                previewWidth,
                previewHeight
            );
            this.addDrawableChild(previewWidget);

        } else {
            // Single column layout: just the options list full-width
            optionListWidget = new OptionListWidget(
                this.client,
                this.width,
                this.height,
                topMargin,
                this.height - bottomMargin,
                optionItemHeight
            );

            // Add all options to the list
            addCustomOptionsToOptionListWidget(optionListWidget);
            this.addDrawableChild(optionListWidget);
        }

        // Add Done button at the bottom
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button ->
            this.close()
        ).dimensions(this.width / 2 - 100, this.height - 27, 200, previewMargin).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render full-screen background first
        this.renderBackground(context);

        // Then render all the widgets (options list, player preview, buttons)
        super.render(context, mouseX, mouseY, delta);

        // Draw the title - centered in options column if player preview exists, otherwise full-screen center
        boolean hasPlayer = MinecraftClient.getInstance().player != null;
        int titleX;

        if (hasPlayer) {
            // Center within the left column (options list area which is 3/5 of screen width)
            int listWidth = (this.width * 3) / 5;
            titleX = listWidth / 2;
        } else {
            // Center across entire screen
            titleX = this.width / 2;
        }

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, titleX, 15, 0xFFFFFF);
    }

    private void addCustomOptionsToOptionListWidget(OptionListWidget optionListWidget) {
        OptionElementFactory optionElementFactory = new OptionElementFactory(
            this,
                optionListWidget,
            this.gameOptions
        );
        
        var helmetOption = optionElementFactory.buildDoubleOption(
            "armorhider.helmet.transparency",
            Text.translatable("armorhider.options.helmet.tooltip"),
            Text.translatable("armorhider.options.helmet.tooltip_narration"),
            currentValue -> Text.translatable("armorhider.options.helmet.button_text",
                String.format("%.0f%%", currentValue * 100)),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetOpacity.getValue(),
            this::setHelmetTransparency
        );
        optionElementFactory.addSimpleOptionAsWidget(helmetOption);

        var skullOrHatOption = optionElementFactory.buildBooleanOption(
            Text.translatable("armorhider.options.helmet_affection.title"),
            Text.translatable("armorhider.options.helmet_affection.tooltip"),
            Text.translatable("armorhider.options.helmet_affection.tooltip_narration"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingHatOrSkull.getValue(),
            this::setOpacityAffectingHatOrSkull
        );
        optionElementFactory.addSimpleOptionAsWidget(skullOrHatOption);

        var chestOption = optionElementFactory.buildDoubleOption(
            "armorhider.chestplate.transparency",
            Text.translatable("armorhider.options.chestplate.tooltip"),
            Text.translatable("armorhider.options.chestplate.tooltip_narration"),
            currentValue -> Text.translatable("armorhider.options.chestplate.button_text",
                String.format("%.0f%%", currentValue * 100)),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestOpacity.getValue(),
            this::setChestTransparency
        );
        optionElementFactory.addSimpleOptionAsWidget(chestOption);

        var elytraOption = optionElementFactory.buildBooleanOption(
            Text.translatable("armorhider.options.elytra_affection.title"),
            Text.translatable("armorhider.options.elytra_affection.tooltip"),
            Text.translatable("armorhider.options.elytra_affection.tooltip_narration"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingElytra.getValue(),
            this::setOpacityAffectingElytra
        );
        optionElementFactory.addSimpleOptionAsWidget(elytraOption);

        var legsOption = optionElementFactory.buildDoubleOption(
            "armorhider.legs.transparency",
            Text.translatable("armorhider.options.leggings.tooltip"),
            Text.translatable("armorhider.options.leggings.tooltip_narration"),
            currentValue -> Text.translatable("armorhider.options.leggings.button_text",
                String.format("%.0f%%", currentValue * 100)),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsOpacity.getValue(),
            this::setLegsTransparency
        );
        optionElementFactory.addSimpleOptionAsWidget(legsOption);

        var bootsOption = optionElementFactory.buildDoubleOption(
            "armorhider.boots.transparency",
            Text.translatable("armorhider.options.boots.tooltip"),
            Text.translatable("armorhider.options.boots.tooltip_narration"),
            currentValue -> Text.translatable("armorhider.options.boots.button_text",
                String.format("%.0f%%", currentValue * 100)),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsOpacity.getValue(),
            this::setBootsTransparency
        );
        optionElementFactory.addSimpleOptionAsWidget(bootsOption);

        SimpleOption<Boolean> enableCombatDetection = optionElementFactory.buildBooleanOption(
            Text.translatable("armorhider.options.combat_detection.title"),
            Text.translatable("armorhider.options.combat_detection.tooltip"),
            Text.translatable("armorhider.options.combat_detection.tooltip_narration"),
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
            SimpleOption<Boolean> combatHidingOnServer = optionElementFactory.buildBooleanOption(
                Text.translatable("armorhider.options.combat_detection_server.title"),
                Text.translatable("armorhider.options.combat_detection_server.tooltip"),
                Text.translatable("armorhider.options.combat_detection_server.tooltip_narration"),
                    serverCombatDetectionValue,
                this::setServerCombatDetection
            );
            optionElementFactory.addSimpleOptionAsWidget(combatHidingOnServer);
        }
    }

    private boolean getFallbackDefault() {
        // Server didn't have the mod, using default value
        hasUsedFallbackWhereServerDidntTranspondSettings = true;
        return true;
    }

    @Override
    public void close() {
        if (settingsChanged) {
            ArmorHider.LOGGER.info("Updating current player settings...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
        }
        if (serverSettingsChanged && !hasUsedFallbackWhereServerDidntTranspondSettings) {
            ArmorHider.LOGGER.info("Updating current server settings (if possible)...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.setAndSendServerCombatDetection(newServerCombatDetection);
        }

        if (this.client != null) {
            this.client.setScreen(this.parent);
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
