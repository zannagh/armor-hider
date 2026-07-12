//? if < 1.21 {
/*package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.elements.ArmorHiderOptionsPanelWidget;
import de.zannagh.armorhider.client.gui.elements.ElementSpacingOptions;
import de.zannagh.armorhider.client.gui.elements.PlayerPreviewWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkinCustomizationScreen.class)
public abstract class SkinCustomizationScreenLegacyMixin extends OptionsSubScreen {

    @Unique
    private boolean armorHider$settingsChanged;

    @Unique
    private ArmorHiderOptionsPanelWidget armorHider$panel;

    @Unique
    private PlayerPreviewWidget armorHider$preview;

    protected SkinCustomizationScreenLegacyMixin(Screen lastScreen, net.minecraft.client.Options options, Component title) {
        super(lastScreen, options, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void armorHider$attachOptionsPanel(CallbackInfo ci) {
        if (!ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().showSettingsInSkinCustomization.getValue()) {
            this.armorHider$panel = null;
            this.armorHider$preview = null;
            return;
        }

        // On 1.20.x vanilla buttons are in a 2-column grid at y = height/6 + 24*row.
        // 7 model-part toggles + 1 mainHand = 8 items → 4 rows (24 px each, 20 px button + 4 px gap).
        // Done button sits right after at row 4.  We relocate Done to the bottom
        // and place the ArmorHider panel between the vanilla options and Done.
        int vanillaTop = this.height / 6;
        int vanillaContentBottom = vanillaTop + 4 * 20 + 3 * 4; // 4 rows of buttons

        // Find and move the Done button to the very bottom
        for (var child : this.children()) {
            if (child instanceof Button btn && btn.getMessage().equals(CommonComponents.GUI_DONE)) {
                btn.setY(this.height - 27);
            }
        }

        int gap = 4;
        int panelY = vanillaContentBottom + gap;
        int panelHeight = this.height - panelY - 37;

        boolean inGame = Minecraft.getInstance().player != null;
        int sectionWidth = 310;
        int sectionLeft = this.width / 2 - sectionWidth / 2;
        var spacing = new ElementSpacingOptions(sectionWidth)
                .forVaryingElements(1, inGame ? 1 : 0)
                .withPercentageWidthForPrimaryElement(60)
                .withGap(0);
        int panelWidth = spacing.getWidth(0);

        this.armorHider$panel = new ArmorHiderOptionsPanelWidget(
                sectionLeft, panelY, panelWidth, panelHeight,
                this, this.options, () -> this.armorHider$settingsChanged = true,
                ArmorHiderClient.PRESET_MANAGER
        );
        this.addRenderableWidget(this.armorHider$panel);

        if (inGame) {
            int previewAreaWidth = spacing.getWidth(1);
            int previewAreaLeft = sectionLeft + spacing.getX(1);
            int contentHeight = this.armorHider$panel.getContentHeight();
            int squareSize = Math.min(contentHeight, Math.min(previewAreaWidth, panelHeight));
            int previewX = previewAreaLeft + (previewAreaWidth - squareSize) / 2;

            this.armorHider$preview = new PlayerPreviewWidget(previewX, panelY, squareSize, squareSize);
            this.addRenderableWidget(this.armorHider$preview);
        } else {
            this.armorHider$preview = null;
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void armorHider$onClose(CallbackInfo ci) {
        if (this.armorHider$settingsChanged) {
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
            this.armorHider$settingsChanged = false;
        }
    }
}
*///?}
