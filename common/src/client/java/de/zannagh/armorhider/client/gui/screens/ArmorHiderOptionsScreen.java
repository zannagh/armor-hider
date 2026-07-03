package de.zannagh.armorhider.client.gui.screens;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.elements.ArmorHiderOptionsPanelWidget;
import de.zannagh.armorhider.client.gui.elements.ElementSpacingOptions;
import de.zannagh.armorhider.client.gui.elements.PlayerPreviewWidget;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class ArmorHiderOptionsScreen extends ArmorHiderConfigurationScreen {
    private final Options gameOptions;

    public ArmorHiderOptionsScreen(@Nullable Screen parent, Options gameOptions) {
        super(parent, gameOptions, Component.translatable("armorhider.options.mod_title"));
        this.gameOptions = gameOptions;
    }

    @Override
    protected void init() {
        boolean inGame = super.isPlayerInGame();
        var spacing = new ElementSpacingOptions(this.width)
                .forVaryingElements(1, inGame ? 1 : 0)
                .withPercentageWidthForPrimaryElement(60)
                .withGap(0);
        int panelWidth = spacing.getWidth(0);
        int panelHeight = this.height - topMargin - bottomMargin;
        var optionsPanelWidget = new ArmorHiderOptionsPanelWidget(0, topMargin, panelWidth, panelHeight, this, this.gameOptions,
                () -> {
                    this.settingsChanged = true;
                    ArmorHiderClient.CLIENT_CONFIG_MANAGER.markLocalDirty();
                }, ArmorHiderClient.PRESET_MANAGER);
        this.addRenderableWidget(optionsPanelWidget);
        this.addRenderableWidget(Button.builder(net.minecraft.network.chat.CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());

        if (inGame) {
            int previewWidth = spacing.getWidth(1) - previewMargin;
            if (previewWidth > 150) {
                previewWidth = 150;
            }
            int previewHeight = previewWidth;
            int previewX = spacing.getX(1) + previewMargin / 2;
            int previewY = topMargin + previewMargin / 2;
            addRenderableWidget(new PlayerPreviewWidget(previewX, previewY, previewWidth, previewHeight));
        }
    }

    @Override
    protected void addOptions() {
        // Options are built by ArmorHiderOptionsPanelWidget in init() on all versions.
    }

    @Override
    protected void saveSettingsOnClose() {
        ArmorHider.LOGGER.info("Updating current player settings...");
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
    }
}
