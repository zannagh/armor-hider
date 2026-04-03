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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

public class ArmorHiderOptionsScreen extends ArmorHiderConfigurationScreen {
    private final Options gameOptions;

    public ArmorHiderOptionsScreen(@Nullable Screen parent, Options gameOptions) {
        super(parent, gameOptions, Component.translatable("armorhider.options.mod_title"));
        this.gameOptions = gameOptions;
    }

    @Override
    protected void init() {
        int listWidth = super.isPlayerInGame() ? (this.width * 3) / 5 : this.width;
        super.initWidgetList(listWidth);
        super.init();

        if (super.isPlayerInGame()) {
            int previewWidth = (this.width * 2) / 5 - previewMargin;
            int previewHeight = this.height - topMargin - bottomMargin - previewMargin * 2;
            int previewX = listWidth + previewMargin / 2;
            int previewY = topMargin + previewMargin / 2;
            addRenderableWidget(new PlayerPreviewWidget(previewX, previewY, previewWidth, previewHeight));
        }
    }

    @Override
    protected void addOptions() {
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue();

        var helmetOption = factory.buildDoubleOption(
                "armorhider.helmet.transparency",
                Component.translatable("armorhider.options.helmet.tooltip"),
                Component.translatable("armorhider.options.helmet.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.helmet.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                config.helmetOpacity.getValue(),
                val -> setSetting(val, config.helmetOpacity::setValue)
        );
        factory.addSliderWithToggles(
                EquipmentSlot.HEAD,
                helmetOption,
                gameOptions,
                config.helmetGlint.getValue(),
                config.opacityAffectingHatOrSkull.getValue(),
                val -> setSetting(val, config.helmetGlint::setValue),
                val -> setSetting(val, config.opacityAffectingHatOrSkull::setValue));

        var chestOption = factory.buildDoubleOption(
                "armorhider.chestplate.transparency",
                Component.translatable("armorhider.options.chestplate.tooltip"),
                Component.translatable("armorhider.options.chestplate.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.chestplate.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                config.chestOpacity.getValue(),
                val -> setSetting(val, config.chestOpacity::setValue));
        factory.addSliderWithToggles(EquipmentSlot.CHEST,
                chestOption,
                gameOptions,
                config.chestGlint.getValue(),
                config.opacityAffectingElytra.getValue(),
                val -> setSetting(val, config.chestGlint::setValue),
                val -> setSetting(val, config.opacityAffectingElytra::setValue));

        var legsOption = factory.buildDoubleOption(
                "armorhider.legs.transparency",
                Component.translatable("armorhider.options.leggings.tooltip"),
                Component.translatable("armorhider.options.leggings.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.leggings.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                config.legsOpacity.getValue(),
                val -> setSetting(val, config.legsOpacity::setValue)
        );
        factory.addSliderWithToggles(EquipmentSlot.LEGS,
                legsOption,
                gameOptions,
                config.legsGlint.getValue(),
                null,
                val -> setSetting(val, config.legsGlint::setValue),
                null);

        var bootsOption = factory.buildDoubleOption(
                "armorhider.boots.transparency",
                Component.translatable("armorhider.options.boots.tooltip"),
                Component.translatable("armorhider.options.boots.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.boots.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                config.bootsOpacity.getValue(),
                val -> setSetting(val, config.bootsOpacity::setValue));
        factory.addSliderWithToggles(EquipmentSlot.FEET,
                bootsOption,
                gameOptions,
                config.bootsGlint.getValue(),
                null,
                val -> setSetting(val, config.bootsGlint::setValue),
                null);

        var offhandOption = factory.buildDoubleOption(
                "armorhider.offhand.transparency",
                Component.translatable("armorhider.options.offhand.tooltip"),
                Component.translatable("armorhider.options.offhand.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.offhand.button_text",
                        String.format("%.0f%%", currentValue * 100)),
                config.offHandOpacity.getValue(),
                val -> setSetting(val, config.offHandOpacity::setValue)
        );
        factory.addSliderWithToggles(EquipmentSlot.OFFHAND,
                offhandOption,
                gameOptions,
                null, null, null, null);

        var enableCombatDetection = factory.buildBooleanOption(
                Component.translatable("armorhider.options.combat_detection.title"),
                Component.translatable("armorhider.options.combat_detection.tooltip"),
                Component.translatable("armorhider.options.combat_detection.tooltip_narration"),
                config.enableCombatDetection.getValue(),
                val -> setSetting(val, config.enableCombatDetection::setValue)
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
    protected void saveSettingsOnClose() {
        ArmorHider.LOGGER.info("Updating current player settings...");
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
    }

    @Override
    public void addWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    @Override
    void removeWidget(AbstractWidget widget) {
        this.removeWidget((GuiEventListener) widget);
    }
}
