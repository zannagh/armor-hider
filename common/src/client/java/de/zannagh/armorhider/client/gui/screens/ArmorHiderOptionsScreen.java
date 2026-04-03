package de.zannagh.armorhider.client.gui.screens;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.elements.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

public class ArmorHiderOptionsScreen extends Screen implements InjectableScreen {
    private final Screen parent;
    private final Options gameOptions;
    private boolean settingsChanged;

    public ArmorHiderOptionsScreen(Screen parent, Options gameOptions) {
        super(Component.translatable("armorhider.options.mod_title"));
        this.parent = parent;
        this.gameOptions = gameOptions;
    }

    @Override
    protected void init() {
        boolean hasPlayer = Minecraft.getInstance().player != null;

        ArmorHider.LOGGER.info("Init options screen.");
        int topMargin = 32;
        int bottomMargin = 32;
        int itemHeight = 25;
        int previewMargin = 20;
        int listWidth = hasPlayer ? (this.width * 3) / 5 : this.width;

        var list = new WidgetList(this.minecraft, listWidth, this.height - topMargin - bottomMargin, topMargin, itemHeight);
        int rowWidth = list.getRowWidth();

        OptionElementFactory factory = new OptionElementFactory(list::addWidget, gameOptions, rowWidth);

        addOptions(factory, rowWidth);
        addRenderableWidget(list);

        if (hasPlayer) {
            int previewWidth = (this.width * 2) / 5 - previewMargin;
            int previewHeight = this.height - topMargin - bottomMargin - previewMargin * 2;
            int previewX = listWidth + previewMargin / 2;
            int previewY = topMargin + previewMargin / 2;
            addRenderableWidget(new PlayerPreviewWidget(previewX, previewY, previewWidth, previewHeight));
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    private void addOptions(OptionElementFactory factory, int rowWidth) {
        
        var helmetOption = factory.buildDoubleOption(
                "armorhider.helmet.transparency",
                Component.translatable("armorhider.options.helmet.tooltip"),
                Component.translatable("armorhider.options.helmet.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.helmet.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetOpacity.getValue(),
                this::setHelmetTransparency
        );
        factory.addElementAsWidget(OptionElementFactory.createSliderWithToggleForSlot(
                EquipmentSlot.HEAD,
                helmetOption,
                gameOptions,
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetGlint.getValue(),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingHatOrSkull.getValue(),
                this::setDisableHelmetGlint,
                this::setOpacityAffectingHatOrSkull,
                rowWidth));

        var chestOption = factory.buildDoubleOption(
                "armorhider.chestplate.transparency",
                Component.translatable("armorhider.options.chestplate.tooltip"),
                Component.translatable("armorhider.options.chestplate.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.chestplate.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestOpacity.getValue(),
                this::setChestTransparency);
        factory.addElementAsWidget(OptionElementFactory.createSliderWithToggleForSlot(EquipmentSlot.CHEST,
                chestOption,
                gameOptions,
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestGlint.getValue(),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingElytra.getValue(),
                this::setDisableChestGlint,
                this::setOpacityAffectingElytra,
                rowWidth));

        var legsOption = factory.buildDoubleOption(
                "armorhider.legs.transparency",
                Component.translatable("armorhider.options.leggings.tooltip"),
                Component.translatable("armorhider.options.leggings.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.leggings.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsOpacity.getValue(),
                this::setLegsTransparency
        );
        factory.addElementAsWidget(OptionElementFactory.createSliderWithToggleForSlot(EquipmentSlot.LEGS,
                legsOption,
                gameOptions,
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsGlint.getValue(),
                null,
                this::setDisableLegsGlint, 
                null,
                rowWidth));

        var bootsOption = factory.buildDoubleOption(
                "armorhider.boots.transparency",
                Component.translatable("armorhider.options.boots.tooltip"),
                Component.translatable("armorhider.options.boots.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.boots.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsOpacity.getValue(),
                this::setBootsTransparency);
        factory.addElementAsWidget(OptionElementFactory.createSliderWithToggleForSlot(EquipmentSlot.FEET,
                bootsOption,
                gameOptions,
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsGlint.getValue(),
                null,
                this::setDisableBootsGlint,
                null,
                rowWidth));

        var offhandOption = factory.buildDoubleOption(
                "armorhider.offhand.transparency",
                Component.translatable("armorhider.options.offhand.tooltip"),
                Component.translatable("armorhider.options.offhand.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.offhand.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().offHandOpacity.getValue(),
                this::setOffhandTransparency
        );
        factory.addSimpleOptionAsWidget(offhandOption);

        var enableCombatDetection = factory.buildBooleanOption(
                Component.translatable("armorhider.options.combat_detection.title"),
                Component.translatable("armorhider.options.combat_detection.tooltip"),
                Component.translatable("armorhider.options.combat_detection.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().enableCombatDetection.getValue(),
                this::setCombatDetection
        );
        factory.addSimpleOptionAsWidget(enableCombatDetection);

        factory.addElementAsWidget(Button.builder(
                Component.translatable("armorhider.options.regular.title"),
                btn -> {
                    //? if >= 1.21.9
                    Minecraft.getInstance().setScreenAndShow(new AdvancedArmorHiderSettingsScreen(this, gameOptions, this.title));
                    //? if < 1.21.9
                    //Minecraft.getInstance().setScreen(new AdvancedArmorHiderSettingsScreen(this, gameOptions, this.title));
                }
        ).tooltip(Tooltip.create(Component.translatable("armorhider.options.regular.title"))).build());
    }

    @Override
    public void onClose() {
        if (settingsChanged) {
            ArmorHider.LOGGER.info("Updating current player settings...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
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

    @Override
    public void addWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    @Override
    public void removeWidget(AbstractWidget widget) {
        this.removeWidget((GuiEventListener) widget);
    }
}
