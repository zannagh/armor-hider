// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.OptionElementFactory;
import de.zannagh.armorhider.gui.AdvancedArmorHiderSettingsScreen;
import de.zannagh.armorhider.rendering.PlayerPreviewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
//? if >= 1.21 {
/*import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
*///?}
//? if < 1.21 {
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.SkinCustomizationScreen;
//?}
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsSubScreen.class)
public abstract class SkinOptionsMixin extends Screen {

    // TODO: This may have to be extended into not sending network stuff if the server doesn't support it.

    //?if >= 1.21 {
    /*
    @Shadow
    protected OptionsList list;
    
    @Final
    @Shadow
    protected Options options;
    *///}
    
    //? if < 1.21 {
    @Unique
    private OptionsList list;
    
    @Mutable
    @Final
    @Shadow
    protected Screen lastScreen;

    @Mutable
    @Final
    @Shadow
    protected Options options;
    
    //?}
    
    @Unique
    private boolean settingsChanged;
    @Unique
    private boolean isSkinOptionsScreen;

    //? if >= 1.21 {
    /*
    protected SkinOptionsMixin(Component component) {
        super(component);
    }
    *///?}
    
    //? if < 1.21 {
    public SkinOptionsMixin(Screen screen, Options options, Component component) {
        super(component);
        this.lastScreen = screen;
        this.options = options;
    }
    //?}
    
    //? if < 1.21 {
    @Inject(
            method = "basicListRender",
            at = @At("HEAD")
    )
    private void interceptBasicListRender(GuiGraphics guiGraphics, OptionsList optionsList, int i, int j, float f, CallbackInfo ci) {
        list = optionsList;
    }

    @Inject(
            method = "basicListRender",
            at = @At("RETURN")
    )
    private void onRenderReturn(GuiGraphics context, OptionsList optionsList, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (list != null && isSkinOptionsScreen) {
            PlayerPreviewRenderer.renderPlayerPreview(context, list, mouseX, mouseY);
        }
    }
    //?}

    //? if >= 1.21 {
    /*@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"))
    private void onRender(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (list != null && isSkinOptionsScreen) {
            PlayerPreviewRenderer.renderPlayerPreview(context, list, mouseX, mouseY);
        }
    }
    *///?}

    @Inject(method = "onClose()V", at = @At("HEAD"))
    private void onCloseHead(CallbackInfo ci) {
        if (!isSkinOptionsScreen) {
            return;
        }
        if (settingsChanged) {
            ArmorHider.LOGGER.info("Updating current player settings...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
        }
    }

    @Inject(
            //?if >= 1.21 {
            // method = "init",
            //?}
            //?if < 1.21 {
            method = "<init>", 
            //?}
            at = @At("RETURN"))
    private void onAddOptions(CallbackInfo ci) {
        Screen self = (Screen)(Object)this;
        isSkinOptionsScreen = Minecraft.getInstance().screen instanceof SkinCustomizationScreen
            || self instanceof SkinCustomizationScreen;

        if (!isSkinOptionsScreen) {
            return;
        }

        // Cast to Screen to avoid mixin class reference in lambda/method reference bytecode
        
        OptionElementFactory optionElementFactory = new OptionElementFactory(self, list, options);
        //? if < 1.21.9 {
        optionElementFactory = optionElementFactory.withWidgetAdder(widget -> addRenderableWidget(widget));
        //?}
        if (Minecraft.getInstance().player != null) {
            optionElementFactory = optionElementFactory.withHalfWidthRendering();
        }

        optionElementFactory.addTextAsWidget(Component.translatable("armorhider.options.mod_title"));

        var helmetOption = optionElementFactory.buildDoubleOption(
                "armorhider.helmet.transparency",
                Component.translatable("armorhider.options.helmet.tooltip"),
                Component.translatable("armorhider.options.helmet.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.helmet.button_text", String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetOpacity.getValue(),
                value -> setHelmetTransparency(value));
        if (Minecraft.getInstance().player != null) {
            //? if >= 1.21.9 {
            /*list.addSmall(OptionElementFactory.simpleOptionToGameOptionWidget(helmetOption, options, list, false),
                    new MultiLineTextWidget(Component.literal("Preview"), this.getFont()));
            *///?}
            //? if < 1.21.9 {
            optionElementFactory.addSimpleOptionAsWidget(helmetOption);
            //?}
        } else {
            optionElementFactory.addSimpleOptionAsWidget(helmetOption);
        }


        var skullOrHatOption = optionElementFactory.buildBooleanOption(
                Component.translatable("armorhider.options.helmet_affection.title"),
                Component.translatable("armorhider.options.helmet_affection.tooltip"),
                Component.translatable("armorhider.options.helmet_affection.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingHatOrSkull.getValue(),
                value -> setOpacityAffectingHatOrSkull(value)
        );
        optionElementFactory.addSimpleOptionAsWidget(skullOrHatOption);

        var chestOption = optionElementFactory.buildDoubleOption(
                "armorhider.chestplate.transparency",
                Component.translatable("armorhider.options.chestplate.tooltip"),
                Component.translatable("armorhider.options.chestplate.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.chestplate.button_text", String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestOpacity.getValue(),
                value -> setChestTransparency(value));
        optionElementFactory.addSimpleOptionAsWidget(chestOption);

        var elytraOption = optionElementFactory.buildBooleanOption(
                Component.translatable("armorhider.options.elytra_affection.title"),
                Component.translatable("armorhider.options.elytra_affection.tooltip"),
                Component.translatable("armorhider.options.elytra_affection.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingElytra.getValue(),
                value -> setOpacityAffectingElytra(value)
        );
        optionElementFactory.addSimpleOptionAsWidget(elytraOption);

        var legsOption = optionElementFactory.buildDoubleOption(
                "armorhider.legs.transparency",
                Component.translatable("armorhider.options.leggings.tooltip"),
                Component.translatable("armorhider.options.leggings.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.leggings.button_text", String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsOpacity.getValue(),
                value -> setLegsTransparency(value));
        optionElementFactory.addSimpleOptionAsWidget(legsOption);

        var bootsOption = optionElementFactory.buildDoubleOption(
                "armorhider.boots.transparency",
                Component.translatable("armorhider.options.boots.tooltip"),
                Component.translatable("armorhider.options.boots.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.boots.button_text", String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsOpacity.getValue(),
                value -> setBootsTransparency(value));
        optionElementFactory.addSimpleOptionAsWidget(bootsOption);

        OptionInstance<Boolean> enableCombatDetection = optionElementFactory.buildBooleanOption(
                Component.translatable("armorhider.options.combat_detection.title"),
                Component.translatable("armorhider.options.combat_detection.tooltip"),
                Component.translatable("armorhider.options.combat_detection.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().enableCombatDetection.getValue(),
                value -> setCombatDetection(value)
        );
        optionElementFactory.addSimpleOptionAsWidget(enableCombatDetection);

        //? if >= 1.21.9 {
        /*optionElementFactory.addElementAsWidget(Button.builder(
                        Component.literal("Advanced..."),
                        (widget) -> Minecraft.getInstance().setScreen(new AdvancedArmorHiderSettingsScreen(Minecraft.getInstance().screen, options, title)))
                .pos(list.getX(), list.getNextY()).size(list.getRowWidth(), Button.DEFAULT_HEIGHT).build());
        *///?}
        //? if < 1.21.9 {
        int rowWidth = de.zannagh.armorhider.rendering.RenderUtilities.getRowWidth(list);
        int rowLeft = de.zannagh.armorhider.rendering.RenderUtilities.getRowLeft(list);
        int nextY = de.zannagh.armorhider.rendering.RenderUtilities.getNextY(list);
        optionElementFactory.addElementAsWidget(Button.builder(
                        Component.literal("Advanced..."),
                        (widget) -> Minecraft.getInstance().setScreen(new AdvancedArmorHiderSettingsScreen(Minecraft.getInstance().screen, options, title)))
                .pos(rowLeft, nextY).size(rowWidth, Button.DEFAULT_HEIGHT).build());
        //?}
    }


    @Unique
    private void setOpacityAffectingHatOrSkull(Boolean value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingHatOrSkull.setValue(value);
        settingsChanged = true;
    }

    @Unique
    private void setOpacityAffectingElytra(Boolean value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().opacityAffectingElytra.setValue(value);
        settingsChanged = true;
    }

    @Unique
    private void setHelmetTransparency(double value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetOpacity.setValue(value);
        settingsChanged = true;
    }

    @Unique
    private void setChestTransparency(double value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestOpacity.setValue(value);
        settingsChanged = true;
    }

    @Unique
    private void setLegsTransparency(double value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsOpacity.setValue(value);
        settingsChanged = true;
    }

    @Unique
    private void setBootsTransparency(double value) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsOpacity.setValue(value);
        settingsChanged = true;
    }

    @Unique
    private void setCombatDetection(boolean enabled) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().enableCombatDetection.setValue(enabled);
        settingsChanged = true;
    }

}
