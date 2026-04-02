package de.zannagh.armorhider.client.gui.screens;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.elements.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;

public class ArmorHiderOptionsScreen extends OptionsSubScreen {
    private final Screen parent;
    private boolean hasUsedFallbackWhereServerDidntTranspondSettings = false;
    private boolean settingsChanged;
    private boolean serverSettingsChanged;
    private boolean newServerCombatDetection;
    //? if < 1.21
    //private OptionsList list;

    public ArmorHiderOptionsScreen(Screen parent, Options gameOptions) {
        super(parent, gameOptions, Component.translatable("armorhider.options.mod_title"));
        this.parent = parent;
    }

    @Override
    //? if < 1.21
    // protected void init() {
    //? if >= 1.21
    protected void addOptions() {
        boolean hasPlayer = Minecraft.getInstance().player != null;

        int topMargin = 32;
        int bottomMargin = 32;
        int optionItemHeight = 25;
        int previewMargin = 20;
        int listWidth = (this.width * 3) / 5;

        //? if < 1.21 {
        /*list = new OptionsList(
                this.minecraft,
                listWidth,
                this.height,
                topMargin,
                this.height - bottomMargin,
                optionItemHeight
        );
        *///?}
        
        if (hasPlayer) {
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
    //? if >= 26.1-1.pre.1 {
    public void extractRenderState(final @NonNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }
    //?}
    
    //? if < 26.1-1.pre.1 {
    /*public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        //? if < 1.21.4 && >= 1.21
        //this.renderBackground(context, mouseX, mouseY, delta);
        //? if < 1.21 {
        /^this.renderBackground(context);
        if (list != null) {
            list.render(context, mouseX, mouseY, delta);
        }
        ^///?}
        super.render(context, mouseX, mouseY, delta);
        
        //? if < 1.21 {
        int titleX;
        /^if (Minecraft.getInstance().player != null) {
        // Center within the left column (options list area which is 3/5 of screen width)
        int listWidth = (this.width * 3) / 5;
        titleX = listWidth / 2;
        } else {
            // Center across entire screen
            titleX = this.width / 2;
        }

        context.drawCenteredString(this.font, this.title, titleX, 15, 0xFFFFFF);
        ^///?}
    }
    *///?}

    private void addCustomOptionsToOptionListWidget(OptionsList optionListWidget, AbstractWidget playerWidget) {
        OptionElementFactory optionElementFactory = new OptionElementFactory(
            this,
            optionListWidget,
            this.options
        );
        int rowWidth = playerWidget == null ? optionListWidget.getRowWidth() : optionListWidget.getRowWidth() / 2;
        var helmetOption = optionElementFactory.buildDoubleOption(
            "armorhider.helmet.transparency",
            Component.translatable("armorhider.options.helmet.tooltip"),
            Component.translatable("armorhider.options.helmet.tooltip_narration"),
            currentValue -> Component.translatable("armorhider.options.helmet.button_text",
                String.format("%.0f%%", currentValue * 100)),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetOpacity.getValue(),
            this::setHelmetTransparency
        );
        var disableHelmetGlint = optionElementFactory.buildBooleanOption(
            Component.translatable("armorhider.options.disable_helmet_glint.title"),
            Component.translatable("armorhider.options.disable_helmet_glint.tooltip"),
            Component.translatable("armorhider.options.disable_helmet_glint.tooltip"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetGlint.getValue(),
            this::setDisableHelmetGlint
        );
        //? if >= 1.21 {
        var compoundHelmet = OptionElementFactory.createSliderWithToggle(helmetOption, disableHelmetGlint, options, rowWidth);
        optionElementFactory.addOptionWithWidget(compoundHelmet, playerWidget);
        //?}
        //? if < 1.21 {
        /*optionElementFactory.addSimpleOptionAsWidget(helmetOption);
        optionElementFactory.addSimpleOptionAsWidget(disableHelmetGlint);
        *///?}

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
        var disableChestGlint = optionElementFactory.buildBooleanOption(
            Component.translatable("armorhider.options.disable_chest_glint.title"),
            Component.translatable("armorhider.options.disable_chest_glint.tooltip"),
            Component.translatable("armorhider.options.disable_chest_glint.tooltip"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestGlint.getValue(),
            this::setDisableChestGlint
        );
        //? if >= 1.21 {
        var compoundChest = OptionElementFactory.createSliderWithToggle(chestOption, disableChestGlint, options, rowWidth);
        optionElementFactory.addElementAsWidget(compoundChest);
        //?}
        //? if < 1.21 {
        /*optionElementFactory.addSimpleOptionAsWidget(chestOption);
        optionElementFactory.addSimpleOptionAsWidget(disableChestGlint);
        *///?}

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
        var disableLegsGlint = optionElementFactory.buildBooleanOption(
            Component.translatable("armorhider.options.disable_legs_glint.title"),
            Component.translatable("armorhider.options.disable_legs_glint.tooltip"),
            Component.translatable("armorhider.options.disable_legs_glint.tooltip"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsGlint.getValue(),
            this::setDisableLegsGlint
        );
        
        //? if >= 1.21 {
        var compoundLegs = OptionElementFactory.createSliderWithToggle(legsOption, disableLegsGlint, options, rowWidth);
        optionElementFactory.addElementAsWidget(compoundLegs);
        //?}
        //? if < 1.21 {
        /*optionElementFactory.addSimpleOptionAsWidget(legsOption);
        optionElementFactory.addSimpleOptionAsWidget(disableLegsGlint);
        *///?}

        var bootsOption = optionElementFactory.buildDoubleOption(
            "armorhider.boots.transparency",
            Component.translatable("armorhider.options.boots.tooltip"),
            Component.translatable("armorhider.options.boots.tooltip_narration"),
            currentValue -> Component.translatable("armorhider.options.boots.button_text",
                String.format("%.0f%%", currentValue * 100)),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsOpacity.getValue(),
            this::setBootsTransparency
        );
        var disableBootsGlint = optionElementFactory.buildBooleanOption(
            Component.translatable("armorhider.options.disable_boots_glint.title"),
            Component.translatable("armorhider.options.disable_boots_glint.tooltip"),
            Component.translatable("armorhider.options.disable_boots_glint.tooltip"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsGlint.getValue(),
            this::setDisableBootsGlint
        );
        //? if >= 1.21 {
        var compoundBoots = OptionElementFactory.createSliderWithToggle(bootsOption, disableBootsGlint, options, rowWidth);
        optionElementFactory.addElementAsWidget(compoundBoots);
        //?}
        //? if < 1.21 {
        /*optionElementFactory.addSimpleOptionAsWidget(bootsOption);
        optionElementFactory.addSimpleOptionAsWidget(disableBootsGlint);
        *///?}

        var offhandOption = optionElementFactory.buildDoubleOption(
                "armorhider.offhand.transparency",
                Component.translatable("armorhider.options.offhand.tooltip"),
                Component.translatable("armorhider.options.offhand.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.offhand.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().offHandOpacity.getValue(),
                this::setOffhandTransparency
        );
        optionElementFactory.addSimpleOptionAsWidget(offhandOption);

        OptionInstance<Boolean> enableCombatDetection = optionElementFactory.buildBooleanOption(
            Component.translatable("armorhider.options.combat_detection.title"),
            Component.translatable("armorhider.options.combat_detection.tooltip"),
            Component.translatable("armorhider.options.combat_detection.tooltip_narration"),
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().enableCombatDetection.getValue(),
            this::setCombatDetection
        );
        optionElementFactory.addSimpleOptionAsWidget(enableCombatDetection);

        Component advancedKey = Component.translatable("armorhider.options.regular.title");
        String advancedKeyString;
        if (advancedKey.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatableContents) {
            advancedKeyString = translatableContents.getKey();
        } else {
            advancedKeyString = advancedKey.getString();
        }
        if (playerWidget != null) {
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
        }
        else {
            optionListWidget.addSmall(
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
                    ), null);
        }
        

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

    private void setOffhandTransparency(double value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().offHandOpacity.setValue(value);
        settingsChanged = true;
    }

    private void setCombatDetection(boolean enabled) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().enableCombatDetection.setValue(enabled);
        settingsChanged = true;
    }

    private void setDisableHelmetGlint(boolean value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetGlint.setValue(value);
        settingsChanged = true;
    }

    private void setDisableChestGlint(boolean value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestGlint.setValue(value);
        settingsChanged = true;
    }

    private void setDisableLegsGlint(boolean value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsGlint.setValue(value);
        settingsChanged = true;
    }

    private void setDisableBootsGlint(boolean value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsGlint.setValue(value);
        settingsChanged = true;
    }

    private void setServerCombatDetection(boolean enabled) {
        newServerCombatDetection = enabled;
        serverSettingsChanged = true;
    }
}