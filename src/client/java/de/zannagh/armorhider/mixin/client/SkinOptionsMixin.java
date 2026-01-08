// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.OptionElementFactory;
import de.zannagh.armorhider.rendering.PlayerPreviewRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptionsScreen.class)
public abstract class SkinOptionsMixin extends Screen {
    
    // TODO: This may have to be extended into not sending network stuff if the server doesn't support it.
    @Unique
    private boolean hasUsedFallbackWhereServerDidntTranspondSettings = false;
    
    @Unique
    private boolean settingsChanged;
    
    @Unique
    private boolean serverSettingsChanged;
    
    @Unique
    private boolean newServerCombatDetection;
    
    @Shadow
    protected OptionListWidget body;
    
    @Final
    @Shadow
    protected GameOptions gameOptions;

    protected SkinOptionsMixin(Text title) {
        super(title);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks){
        super.render(context, mouseX, mouseY, deltaTicks);
        if (body != null) {
            PlayerPreviewRenderer.renderPlayerPreview(context, body, mouseX, mouseY);
        }
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
        super.close();
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onAddOptions(CallbackInfo ci) {

        OptionElementFactory optionElementFactory = new OptionElementFactory(this, body, gameOptions);
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
    
    @Unique
    private boolean getFallbackDefault() {
        // Server didn't have the mod, using default value
        hasUsedFallbackWhereServerDidntTranspondSettings = true;
        return true;
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

    @Unique
    private void setServerCombatDetection(boolean enabled) {
        newServerCombatDetection = enabled;
        serverSettingsChanged = true;
    }
}
