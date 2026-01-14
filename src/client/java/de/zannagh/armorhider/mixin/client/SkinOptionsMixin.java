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
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsSubScreen.class)
public abstract class SkinOptionsMixin extends Screen {

    // TODO: This may have to be extended into not sending network stuff if the server doesn't support it.
    
    @Unique
    private boolean settingsChanged;

    @Unique
    private boolean isSkinOptionsScreen;

    @Shadow
    protected OptionsList list;

    @Final
    @Shadow
    protected Options options;

    protected SkinOptionsMixin(Component component) {
        super(component);
    }


    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks){
        super.render(context, mouseX, mouseY, deltaTicks);
        if (list != null && isSkinOptionsScreen) {
            PlayerPreviewRenderer.renderPlayerPreview(context, list, mouseX, mouseY);
        }
    }

    @Override
    public void close() {
        if (!isSkinOptionsScreen) {
            super.close();
            return;
        }
        if (settingsChanged) {
            ArmorHider.LOGGER.info("Updating current player settings...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
        }
        super.close();
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onAddOptions(CallbackInfo ci) {
        isSkinOptionsScreen = MinecraftClient.getInstance().currentScreen instanceof SkinCustomizationScreen;

        if (!isSkinOptionsScreen) {
            return;
        }

        OptionElementFactory optionElementFactory = new OptionElementFactory(this, body, options);
        if (MinecraftClient.getInstance().player != null) {
            optionElementFactory = optionElementFactory.withHalfWidthRendering();
        }

        optionElementFactory.addTextAsWidget(Text.translatable("armorhider.options.mod_title"));

        var helmetOption = optionElementFactory.buildDoubleOption(
                "armorhider.helmet.transparency",
                Text.translatable("armorhider.options.helmet.tooltip"),
                Text.translatable("armorhider.options.helmet.tooltip_narration"),
                currentValue -> Text.translatable("armorhider.options.helmet.button_text", String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetOpacity.getValue(),
                this::setHelmetTransparency);
        if (MinecraftClient.getInstance().player != null) {
            body.addWidgetEntry(OptionElementFactory.simpleOptionToGameOptionWidget(helmetOption, options, body, false), 
                    new TextWidget(Text.literal("Preview"), this.getTextRenderer()));
        }
        else {
            optionElementFactory.addSimpleOptionAsWidget(helmetOption);
        }
        

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
                currentValue -> Text.translatable("armorhider.options.chestplate.button_text", String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestOpacity.getValue(),
                this::setChestTransparency);
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
                currentValue -> Text.translatable("armorhider.options.leggings.button_text", String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsOpacity.getValue(),
                this::setLegsTransparency);
        optionElementFactory.addSimpleOptionAsWidget(legsOption);

        var bootsOption = optionElementFactory.buildDoubleOption(
                "armorhider.boots.transparency",
                Text.translatable("armorhider.options.boots.tooltip"),
                Text.translatable("armorhider.options.boots.tooltip_narration"),
                currentValue -> Text.translatable("armorhider.options.boots.button_text", String.format("%.0f%%", currentValue * 100)),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsOpacity.getValue(),
                this::setBootsTransparency);
        optionElementFactory.addSimpleOptionAsWidget(bootsOption);

        SimpleOption<Boolean> enableCombatDetection = optionElementFactory.buildBooleanOption(
                Text.translatable("armorhider.options.combat_detection.title"),
                Text.translatable("armorhider.options.combat_detection.tooltip"),
                Text.translatable("armorhider.options.combat_detection.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().enableCombatDetection.getValue(),
                this::setCombatDetection
        );
        optionElementFactory.addSimpleOptionAsWidget(enableCombatDetection);
        
        optionElementFactory.addElementAsWidget(ButtonWidget.builder(
                Text.literal("Advanced..."), 
                (widget) -> MinecraftClient.getInstance().setScreen(new AdvancedArmorHiderSettingsScreen(MinecraftClient.getInstance().currentScreen, options, title)))
                .dimensions(body.getX(), body.getYOfNextEntry(), body.getRowWidth(), ButtonWidget.DEFAULT_HEIGHT).build());
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
    private void setHelmetTransparency(double value){
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().helmetOpacity.setValue(value);
        settingsChanged = true;
    }

    @Unique
    private void setChestTransparency(double value){
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().chestOpacity.setValue(value);
        settingsChanged = true;
    }

    @Unique
    private void setLegsTransparency(double value){
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().legsOpacity.setValue(value);
        settingsChanged = true;
    }

    @Unique
    private void setBootsTransparency(double value){
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().bootsOpacity.setValue(value);
        settingsChanged = true;
    }

    @Unique
    private void setCombatDetection(boolean enabled) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().enableCombatDetection.setValue(enabled);
        settingsChanged = true;
    }

}
