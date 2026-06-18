package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.ArmorHiderClient;
//? if >= 1.21 {
import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.elements.ArmorHiderOptionsPanelWidget;
import de.zannagh.armorhider.client.gui.elements.ElementSpacingOptions;
import de.zannagh.armorhider.client.gui.elements.PlayerPreviewWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
//?}
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21 {
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
//?} else {
/*import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
*///?}

@Mixin(OptionsSubScreen.class)
public abstract class SkinCustomizationScreenMixin extends Screen {

    //? if >= 1.21 {
    @Shadow
    protected OptionsList list;

    @Shadow
    protected Options options;

    @Shadow
    public HeaderAndFooterLayout layout;
    //?}

    //? if >= 1.21 {
    @Unique
    private boolean armorHider$settingsChanged;

    @Unique
    private ArmorHiderOptionsPanelWidget armorHider$panel;

    @Unique
    private PlayerPreviewWidget armorHider$preview;
    //?}

    protected SkinCustomizationScreenMixin(Component title) {
        super(title);
    }

    //? if >= 1.21 {
    @Inject(method = "init", at = @At("TAIL"))
    private void armorHider$attachOptionsPanel(CallbackInfo ci) {
        if (!((Object) this instanceof SkinCustomizationScreen)) return;
        if (!ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().showSettingsInSkinCustomization.getValue()) {
            this.armorHider$panel = null;
            this.armorHider$preview = null;
            return;
        }

        boolean inGame = Minecraft.getInstance().player != null;
        int sectionWidth = this.list.getWidth();
        var spacing = new ElementSpacingOptions(sectionWidth)
                .forVaryingElements(1, inGame ? 1 : 0)
                .withPercentageWidthForPrimaryElement(60)
                .withGap(0);
        int panelWidth = spacing.getWidth(0);

        this.armorHider$panel = new ArmorHiderOptionsPanelWidget(
                0, 0, panelWidth, 100,
                this, this.options, () ->
                {
                    this.armorHider$settingsChanged = true;
                    ArmorHiderClient.CLIENT_CONFIG_MANAGER.markLocalDirty();
                },
                ArmorHiderClient.PRESET_MANAGER
        );
        this.addRenderableWidget(this.armorHider$panel);

        if (inGame) {
            this.armorHider$preview = new PlayerPreviewWidget(0, 0, 0, 0);
            this.addRenderableWidget(this.armorHider$preview);
        } else {
            this.armorHider$preview = null;
        }

        armorHider$layoutPanel();
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void armorHider$repositionPanel(CallbackInfo ci) {
        if (!((Object) this instanceof SkinCustomizationScreen)) return;
        armorHider$layoutPanel();
    }

    @Unique
    private void armorHider$layoutPanel() {
        if (this.armorHider$panel == null) return;

        int listBottom = this.list.getY() + 4 * UiConstants.DEFAULT_BUTTON_HEIGHT + 3 * UiConstants.DEFAULT_BUTTON_SPACING;
        int gap = 4;
        int panelY = listBottom + gap;
        int panelHeight = this.height - panelY - 37;
        int sectionWidth = this.list.getWidth();
        int sectionLeft = this.width / 2 - sectionWidth / 2;

        this.list.setHeight(listBottom - this.list.getY());

        if (this.armorHider$preview != null) {
            var layoutSpacing = new ElementSpacingOptions(sectionWidth)
                    .forVaryingElements(1, 1)
                    .withPercentageWidthForPrimaryElement(60)
                    .withGap(0);
            int optionsPanelWidth = layoutSpacing.getWidth(0);
            int previewAreaWidth = layoutSpacing.getWidth(1);

            this.armorHider$panel.setX(sectionLeft);
            this.armorHider$panel.setY(panelY);
            this.armorHider$panel.setWidth(optionsPanelWidth);
            this.armorHider$panel.setHeight(panelHeight);

            int contentHeight = this.armorHider$panel.getContentHeight();
            int previewAreaLeft = sectionLeft + layoutSpacing.getX(1) + 40;
            int squareSize = Math.min(contentHeight, Math.min(previewAreaWidth, panelHeight));
            this.armorHider$preview.setX(previewAreaLeft);
            this.armorHider$preview.setY(panelY);
            this.armorHider$preview.setWidth(squareSize);
            this.armorHider$preview.setHeight(squareSize);
        } else {
            this.armorHider$panel.setX(sectionLeft);
            this.armorHider$panel.setY(panelY);
            this.armorHider$panel.setWidth(sectionWidth);
            this.armorHider$panel.setHeight(panelHeight);
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void armorHider$onClose(CallbackInfo ci) {
        if (!((Object) this instanceof SkinCustomizationScreen)) return;
        if (this.armorHider$settingsChanged) {
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
            this.armorHider$settingsChanged = false;
        }
    }
    //?}
}
