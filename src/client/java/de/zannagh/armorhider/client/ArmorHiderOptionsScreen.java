// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import net.minecraft.client.MinecraftClient;
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
    private OptionListWidget optionListWidget;

    private boolean settingsChanged;
    private boolean serverSettingsChanged;
    private boolean newServerCombatDetection;

    public ArmorHiderOptionsScreen(Screen parent, GameOptions gameOptions) {
        super(parent, gameOptions, Text.translatable("armorhider.options.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Create the option list widget
        this.optionListWidget = new OptionListWidget(
            this.client,
            this.width,
            this.height,
            32, // top
            this.height - 32, // bottom
            25  // itemHeight
        );

        // Add all our custom options
        addCustomOptions();

        // Add the widget to the screen (needs to be both drawable and selectable)
        this.addDrawableChild(this.optionListWidget);

        // Add Done button at the bottom
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> {
            this.close();
        }).dimensions(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    private void addCustomOptions() {
        OptionElementFactory optionElementFactory = new OptionElementFactory(
            this,
            this.optionListWidget,
            this.gameOptions
        );

        // Note: Half-width rendering and player preview are disabled in 1.20.1
        // due to API limitations with OptionListWidget

        // Add title (no-op in 1.20.1 but kept for future compatibility)
        optionElementFactory.addTextAsWidget(Text.translatable("armorhider.options.mod_title"));

        // Helmet transparency option
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

        // Skull/Hat affection option
        var skullOrHatOption = optionElementFactory.buildBooleanOption(
            Text.translatable("armorhider.options.helmet_affection.title"),
            Text.translatable("armorhider.options.helmet_affection.tooltip"),
            Text.translatable("armorhider.options.helmet_affection.tooltip_narration"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingHatOrSkull.getValue(),
            this::setOpacityAffectingHatOrSkull
        );
        optionElementFactory.addSimpleOptionAsWidget(skullOrHatOption);

        // Chestplate transparency option
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

        // Elytra affection option
        var elytraOption = optionElementFactory.buildBooleanOption(
            Text.translatable("armorhider.options.elytra_affection.title"),
            Text.translatable("armorhider.options.elytra_affection.tooltip"),
            Text.translatable("armorhider.options.elytra_affection.tooltip_narration"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingElytra.getValue(),
            this::setOpacityAffectingElytra
        );
        optionElementFactory.addSimpleOptionAsWidget(elytraOption);

        // Leggings transparency option
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

        // Boots transparency option
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

        // Combat detection option
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
            SimpleOption<Boolean> combatHidingOnServer = optionElementFactory.buildBooleanOption(
                Text.translatable("armorhider.options.combat_detection_server.title"),
                Text.translatable("armorhider.options.combat_detection_server.tooltip"),
                Text.translatable("armorhider.options.combat_detection_server.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig().serverWideSettings.enableCombatDetection.getValue(),
                this::setServerCombatDetection
            );
            optionElementFactory.addSimpleOptionAsWidget(combatHidingOnServer);
        }
    }

    @Override
    public void close() {
        // Save settings if they changed
        if (settingsChanged) {
            ArmorHider.LOGGER.info("Updating current player settings...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
        }
        if (serverSettingsChanged) {
            ArmorHider.LOGGER.info("Updating current server settings (if possible)...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.setAndSendServerCombatDetection(newServerCombatDetection);
        }

        // Close and return to parent
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    // Setter methods for options
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
