package de.zannagh.armorhider.gui;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.OptionElementFactory;
import de.zannagh.armorhider.rendering.RenderUtilities;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;

public class AdvancedArmorHiderSettingsScreen extends GameOptionsScreen {

    private boolean hasUsedFallbackWhereServerDidntTranspondSettings = false;

    private boolean serverSettingsChanged;

    private boolean newServerCombatDetection;
    
    private boolean setForceArmorHiderOff;
    
    private boolean localSettingsChanged;
    
    private boolean setDisableOthers;
    
    private boolean setUseLocalSettingsForOthersWhenUnknown;
    
    private boolean setDisableLocal;
    
    public AdvancedArmorHiderSettingsScreen(Screen parent, GameOptions gameOptions, Text title) {
        super(parent, gameOptions, title);
    }

    
    @Override
    protected void addOptions() {
        OptionElementFactory optionElementFactory = new OptionElementFactory(this, body, gameOptions);
        
        var adminCategory = new TextWidget(RenderUtilities.getRowWidth(body), 20, Text.translatable("armorhider.options.admin.title"), this.textRenderer);
        optionElementFactory.addElementAsWidget(adminCategory);
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        boolean serverCombatDetectionValue = serverConfig != null
                && serverConfig.serverWideSettings != null
                && serverConfig.serverWideSettings.enableCombatDetection != null
                ? serverConfig.serverWideSettings.enableCombatDetection.getValue()
                : getFallbackDefault(true);
        
        boolean serverForcingArmorHiderOffValue = serverConfig != null
                && serverConfig.serverWideSettings != null
                && serverConfig.serverWideSettings.forceArmorHiderOff != null
                ? serverConfig.serverWideSettings.forceArmorHiderOff.getValue()
                : getFallbackDefault(false);
        
        var onText = Text.translatable("armorhider.options.toggle.on");
        var offText = Text.translatable("armorhider.options.toggle.off");
        var combatDetectionServerText = Text.translatable("armorhider.options.combat_detection_server.title");
        var forceArmorHiderOffText = Text.translatable("armorhider.options.force_armor_hider_off.title");
        
        var cyclingWidgetBuilder = CyclingButtonWidget.onOffBuilder(
                ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin ? onText : CyclingButtonWidget.makeInactive(onText),
                ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin ? offText : CyclingButtonWidget.makeInactive(offText),
                serverCombatDetectionValue
        );

        var forceOnOffBuilder = CyclingButtonWidget.onOffBuilder(
                ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin ? onText : CyclingButtonWidget.makeInactive(onText),
                ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin ? offText : CyclingButtonWidget.makeInactive(offText),
                serverForcingArmorHiderOffValue
        );
        
        var cyclingWidget = cyclingWidgetBuilder.tooltip(_ ->{
            if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                return Tooltip.of(Text.translatable("armorhider.options.combat_detection_server.tooltip.disabled"));
            }
            return Tooltip.of(Text.translatable("armorhider.options.combat_detection_server.tooltip"));
        }).build(
                ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin ? combatDetectionServerText : CyclingButtonWidget.makeInactive(combatDetectionServerText),
                (widget, newValue) -> {
                    if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                        widget.setValue(serverCombatDetectionValue);
                        return;
                    }
                    setServerCombatDetection(newValue);
                }
        );

        var armorHiderOffWidget = forceOnOffBuilder.tooltip(_ ->{
            if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                return Tooltip.of(Text.translatable("armorhider.options.force_armor_hider_off.tooltip.disabled"));
            }
            return Tooltip.of(Text.translatable("armorhider.options.force_armor_hider_off.tooltip"));
        }).build(
                ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin ? forceArmorHiderOffText : CyclingButtonWidget.makeInactive(forceArmorHiderOffText),
                (widget, newValue) -> {
                    if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                        widget.setValue(serverForcingArmorHiderOffValue);
                        return;
                    }
                    setForceArmorHiderOff(newValue);
                }
        );
        
        cyclingWidget.setDimensions(RenderUtilities.getRowWidth(body), 20);
        armorHiderOffWidget.setDimensions(RenderUtilities.getRowWidth(body), 20);
        
        optionElementFactory.addElementAsWidget(cyclingWidget);
        optionElementFactory.addElementAsWidget(armorHiderOffWidget);

        var regularCategory = new TextWidget(RenderUtilities.getRowWidth(body), 20, Text.translatable("armorhider.options.regular.title"), this.textRenderer);
        optionElementFactory.addElementAsWidget(regularCategory);
        
        var settingsToUse = optionElementFactory.buildBooleanOption(
                Text.translatable("armorhider.options.use_settings_for_unknown.title"),
                Text.translatable("armorhider.options.use_settings_for_unknown.tooltip"),
                Text.translatable("armorhider.options.use_settings_for_unknown.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().usePlayerSettingsWhenUndeterminable.getValue(),
                this::setUseLocalSettingsForOthersWhenUnknown
        );

        var globalToggle = optionElementFactory.buildBooleanOption(
                Text.translatable("armorhider.options.disable_local.title"),
                Text.translatable("armorhider.options.disable_local.tooltip"),
                Text.translatable("armorhider.options.disable_local.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.getValue(),
                this::setDisableLocal
        );
        
        var otherToggle = optionElementFactory.buildBooleanOption(
                Text.translatable("armorhider.options.disable_others.title"),
                Text.translatable("armorhider.options.disable_others.tooltip"),
                Text.translatable("armorhider.options.disable_others.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHiderForOthers.getValue(),
                this::setDisableOthers
        );
        
        optionElementFactory.addSimpleOptionAsWidget(settingsToUse);
        optionElementFactory.addSimpleOptionAsWidget(globalToggle);
        optionElementFactory.addSimpleOptionAsWidget(otherToggle);
    }
    
    @Override
    public void close(){
        if (serverSettingsChanged && !hasUsedFallbackWhereServerDidntTranspondSettings) {
            ArmorHider.LOGGER.info("Updating current server settings (if possible)...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.setAndSendServerConfig(newServerCombatDetection, setForceArmorHiderOff);
        }
        if (localSettingsChanged) {
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHiderForOthers.setValue(setDisableOthers);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.setValue(setDisableLocal);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().usePlayerSettingsWhenUndeterminable.setValue(setUseLocalSettingsForOthersWhenUnknown);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
        }
        super.close();
    }

    private boolean getFallbackDefault(boolean valueToReturn) {
        // Server didn't have the mod, using default value
        hasUsedFallbackWhereServerDidntTranspondSettings = true;
        return valueToReturn;
    }

    private void setServerCombatDetection(boolean enabled) {
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            return;
        }
        newServerCombatDetection = enabled;
        serverSettingsChanged = true;
    }

    private void setForceArmorHiderOff(boolean enabled) {
        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            return;
        }
        setForceArmorHiderOff = enabled;
        serverSettingsChanged = true;
    }
    
    private void setDisableOthers(boolean enabled){
        setDisableOthers = enabled;
        localSettingsChanged = true;
    }
    
    private void setUseLocalSettingsForOthersWhenUnknown(boolean enabled){
        setUseLocalSettingsForOthersWhenUnknown = enabled;
        localSettingsChanged = true;
    }
    
    private void setDisableLocal(boolean enabled){
        setDisableLocal = enabled;
        localSettingsChanged = true;
    }
}
