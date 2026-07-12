package de.zannagh.armorhider.client.gui.screens;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.log.DebugLogger;
import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdvancedArmorHiderSettingsScreen extends ArmorHiderConfigurationScreen {
    private boolean hasUsedFallbackWhereServerDidntTranspondSettings = false;
    private boolean serverSettingsChanged;
    private boolean newServerCombatDetection;
    private boolean setForceArmorHiderOff;
    private boolean localSettingsChanged;
    private boolean setShowSettingsInSkinCustomization = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().showSettingsInSkinCustomization.getValue();
    private boolean setDisableLocal = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().disableArmorHider.getValue();
    private boolean forceServerOffDefaultSetting;
    private boolean combatDetectionDefaultSetting;

    private boolean visibilityRespectDefaultSetting;
    private boolean allowIndividualConfigsDefaultSetting;
    private boolean setAllowIndividualConfigs;
    private Button debugButton;

    public AdvancedArmorHiderSettingsScreen(Screen parent, Options gameOptions, Component title) {
        super(parent, gameOptions, title);
        this.gameOptions = gameOptions;
    }

    @Override
    protected void init() {
        super.initWidgetList(this.width);
        super.init();
    }

    @Override
    protected void addOptions() {
        var onText = Component.translatable("armorhider.options.toggle.on");
        var offText = Component.translatable("armorhider.options.toggle.off");
        
        factory.addTextWidget(Component.translatable("armorhider.options.admin.title"));

        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        combatDetectionDefaultSetting = serverConfig != null
                ? serverConfig.serverWideSettings.enableCombatDetection.getValue()
                : getFallbackDefault(true);

        forceServerOffDefaultSetting = serverConfig != null
                ? serverConfig.serverWideSettings.forceArmorHiderOff.getValue()
                : getFallbackDefault(false);

        visibilityRespectDefaultSetting = serverConfig != null
                ? serverConfig.serverWideSettings.disableArmorHiderOnInvisibilityGlobally.getValue()
                : getFallbackDefault(false);

        allowIndividualConfigsDefaultSetting = serverConfig != null
                ? serverConfig.serverWideSettings.allowIndividualPlayerConfigurations.getValue()
                : getFallbackDefault(true);
        setAllowIndividualConfigs = allowIndividualConfigsDefaultSetting;

        var combatDetectionServerText = Component.translatable("armorhider.options.combat_detection_server.title");
        var forceArmorHiderOffText = Component.translatable("armorhider.options.force_armor_hider_off.title");
        var invisibilityRespectServerText = Component.translatable("armorhider.options.invisibility_respect_server.title");
        var allowIndividualConfigsText = Component.translatable("armorhider.options.other_player_server.title");

        //? if >= 1.21.9 {
        //? if >= 1.21.11
        var cyclingWidgetBuilder = CycleButton.booleanBuilder(onText, offText, combatDetectionDefaultSetting);
        //? if >= 1.21.9 && < 1.21.11
        //var cyclingWidgetBuilder = CycleButton.booleanBuilder(onText, offText).withInitialValue(combatDetectionDefaultSetting);
        //? if >= 1.21.11
        var forceOnOffBuilder = CycleButton.booleanBuilder(onText, offText, forceServerOffDefaultSetting);
        //? if >= 1.21.9 && < 1.21.11
        //var forceOnOffBuilder = CycleButton.booleanBuilder(onText, offText).withInitialValue(forceServerOffDefaultSetting);
        //? if >= 1.21.11
        var visibilityRespectBuilder = CycleButton.booleanBuilder(onText, offText, visibilityRespectDefaultSetting);
        //? if >= 1.21.9 && < 1.21.11
        //var visibilityRespectBuilder = CycleButton.booleanBuilder(onText, offText).withInitialValue(visibilityRespectDefaultSetting);
        //? if >= 1.21.11
        var allowIndividualConfigsBuilder = CycleButton.booleanBuilder(onText, offText, allowIndividualConfigsDefaultSetting);
        //? if >= 1.21.9 && < 1.21.11
        //var allowIndividualConfigsBuilder = CycleButton.booleanBuilder(onText, offText).withInitialValue(allowIndividualConfigsDefaultSetting);

        var combatButton = cyclingWidgetBuilder.withTooltip(newValue -> {
            if (ArmorHiderClient.permissionLevel < 3) {
                return Tooltip.create(Component.translatable("armorhider.options.combat_detection_server.tooltip.disabled"));
            }
            return Tooltip.create(Component.translatable("armorhider.options.combat_detection_server.tooltip"));
        }).create(
                combatDetectionServerText,
                (widget, newValue) -> {
                    if (ArmorHiderClient.permissionLevel < 3) {
                        widget.setValue(combatDetectionDefaultSetting);
                        return;
                    }
                    setSetting(newValue, val -> {
                        this.newServerCombatDetection = val;
                        serverSettingsChanged = true;
                    });
                }
        );

        var armorHiderOffButton = forceOnOffBuilder.withTooltip(newValue -> {
            if (ArmorHiderClient.permissionLevel < 3) {
                return Tooltip.create(Component.translatable("armorhider.options.force_armor_hider_off.tooltip.disabled"));
            }
            return Tooltip.create(Component.translatable("armorhider.options.force_armor_hider_off.tooltip"));
        }).create(
                forceArmorHiderOffText,
                (widget, newValue) -> {
                    if (ArmorHiderClient.permissionLevel < 3) {
                        widget.setValue(forceServerOffDefaultSetting);
                        return;
                    }
                    setSetting(newValue, val -> {
                        this.setForceArmorHiderOff = val;
                        serverSettingsChanged = true;
                    });
                }
        );

        var visibilityButton = visibilityRespectBuilder.withTooltip(newValue -> {
            if (ArmorHiderClient.permissionLevel < 3) {
                return Tooltip.create(Component.translatable("armorhider.options.invisibility_respect_server.tooltip.disabled"));
            }
            return Tooltip.create(Component.translatable("armorhider.options.invisibility_respect_server.tooltip"));
        }).create(
                invisibilityRespectServerText,
                (widget, newValue) -> {
                    if (ArmorHiderClient.permissionLevel < 3) {
                        widget.setValue(visibilityRespectDefaultSetting);
                        return;
                    }
                    setSetting(newValue, val -> {
                        this.visibilityRespectDefaultSetting = val;
                        serverSettingsChanged = true;
                    });
                }
        );

        var allowIndividualConfigsButton = allowIndividualConfigsBuilder.withTooltip(newValue -> {
            if (ArmorHiderClient.permissionLevel < 3) {
                return Tooltip.create(Component.translatable("armorhider.options.other_player_server.tooltip.disabled"));
            }
            return Tooltip.create(Component.translatable("armorhider.options.other_player_server.tooltip"));
        }).create(
                allowIndividualConfigsText,
                (widget, newValue) -> {
                    if (ArmorHiderClient.permissionLevel < 3) {
                        widget.setValue(allowIndividualConfigsDefaultSetting);
                        return;
                    }
                    setSetting(newValue, val -> {
                        this.setAllowIndividualConfigs = val;
                        serverSettingsChanged = true;
                    });
                }
        );
        //?}

        //? if < 1.21.9 {
        
        /*OptionInstance<Boolean> combatDetectionServerOption = factory.buildBooleanOption(
                combatDetectionServerText,
                ArmorHiderClient.permissionLevel >= 3
                        ? Component.translatable("armorhider.options.combat_detection_server.tooltip")
                        : Component.translatable("armorhider.options.combat_detection_server.tooltip.disabled"),
                null,
                combatDetectionDefaultSetting,
                val -> {
                    if (ArmorHiderClient.permissionLevel < 3) {
                        return;
                    }
                    setSetting(val, v -> {
                        this.newServerCombatDetection = v;
                        serverSettingsChanged = true;
                    });
                }
        );

        OptionInstance<Boolean> forceOffOption = factory.buildBooleanOption(
                forceArmorHiderOffText,
                ArmorHiderClient.permissionLevel >= 3
                        ? Component.translatable("armorhider.options.force_armor_hider_off.tooltip")
                        : Component.translatable("armorhider.options.force_armor_hider_off.tooltip.disabled"),
                null,
                forceServerOffDefaultSetting,
                val -> {
                    if (ArmorHiderClient.permissionLevel < 3) {
                        return;
                    }
                    setSetting(val, v -> {
                        this.setForceArmorHiderOff = v;
                        serverSettingsChanged = true;
                    });
                }
        );

        OptionInstance<Boolean> visibilityRespectOption = factory.buildBooleanOption(
                invisibilityRespectServerText,
                ArmorHiderClient.permissionLevel >= 3
                        ? Component.translatable("armorhider.options.invisibility_respect_server.tooltip")
                        : Component.translatable("armorhider.options.invisibility_respect_server.tooltip.disabled"),
                null,
                visibilityRespectDefaultSetting,
                val -> {
                    if (ArmorHiderClient.permissionLevel < 3) {
                        return;
                    }
                    setSetting(val, v -> {
                        this.visibilityRespectDefaultSetting = v;
                        serverSettingsChanged = true;
                    });
                }
        );

        OptionInstance<Boolean> allowIndividualConfigsOption = factory.buildBooleanOption(
                allowIndividualConfigsText,
                ArmorHiderClient.permissionLevel >= 3
                        ? Component.translatable("armorhider.options.other_player_server.tooltip")
                        : Component.translatable("armorhider.options.other_player_server.tooltip.disabled"),
                null,
                allowIndividualConfigsDefaultSetting,
                val -> {
                    if (ArmorHiderClient.permissionLevel < 3) {
                        return;
                    }
                    setSetting(val, v -> {
                        this.setAllowIndividualConfigs = v;
                        serverSettingsChanged = true;
                    });
                }
        );

        var combatButton = combatDetectionServerOption.createButton(gameOptions, 0, 0, rowWidth);
        var armorHiderOffButton = forceOffOption.createButton(gameOptions, 0, 0, rowWidth);
        var visibilityButton = visibilityRespectOption.createButton(gameOptions, 0, 0, rowWidth);
        var allowIndividualConfigsButton = allowIndividualConfigsOption.createButton(gameOptions, 0, 0, rowWidth);
        *///?}

        combatButton.active = ArmorHiderClient.permissionLevel >= 3;
        armorHiderOffButton.active = ArmorHiderClient.permissionLevel >= 3;
        visibilityButton.active = ArmorHiderClient.permissionLevel >= 3;
        allowIndividualConfigsButton.active = ArmorHiderClient.permissionLevel >= 3;

        factory.addElementAsWidget(combatButton);
        factory.addElementAsWidget(armorHiderOffButton);
        factory.addElementAsWidget(visibilityButton);
        factory.addElementAsWidget(allowIndividualConfigsButton);

        factory.addTextWidget(Component.translatable("armorhider.options.regular.title"));

        // The "apply settings to unknown players" and "disable for others" toggles moved to the
        // Global Configuration tab of the Individual Player Configurations screen.
        var globalToggle = factory.buildBooleanOption(
                Component.translatable("armorhider.options.disable_local.title"),
                Component.translatable("armorhider.options.disable_local.tooltip"),
                Component.translatable("armorhider.options.disable_local.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().disableArmorHider.getValue(),
                val -> setSetting(val, v -> {
                    setDisableLocal = v;
                    localSettingsChanged = true;
                })
        );

        var settingsLocationToggle = factory.buildBooleanOption(
                Component.translatable("armorhider.options.show_settings_in_skin.title"),
                Component.translatable("armorhider.options.show_settings_in_skin.tooltip"),
                Component.translatable("armorhider.options.show_settings_in_skin.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().showSettingsInSkinCustomization.getValue(),
                val -> setSetting(val, v -> {
                    setShowSettingsInSkinCustomization = v;
                    localSettingsChanged = true;
                })
        );
        factory.addSimpleOptionAsWidget(globalToggle);
        factory.addSimpleOptionAsWidget(settingsLocationToggle);

        factory.addTextWidget(Component.translatable("armorhider.options.debug.title"));

        debugButton = Button.builder(
                getDebugButtonText(),
                btn -> {
                    ArmorHiderClient.toggleDebugLogging();
                    btn.setMessage(getDebugButtonText());
                })
                .tooltip(Tooltip.create(Component.translatable("armorhider.options.debug.tooltip")))
                .build();
        factory.addElementAsWidget(debugButton);
    }

    @Override
    protected void saveSettingsOnClose() {
        if (!hasUsedFallbackWhereServerDidntTranspondSettings && serverSettingsChanged) {
            ArmorHider.LOGGER.info("Updating current server settings (if possible)...");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.setAndSendServerConfig(newServerCombatDetection, setForceArmorHiderOff, visibilityRespectDefaultSetting, setAllowIndividualConfigs);
        }
        if (localSettingsChanged) {
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().disableArmorHider.setValue(setDisableLocal);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().showSettingsInSkinCustomization.setValue(setShowSettingsInSkinCustomization);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
        }
    }

    private boolean getFallbackDefault(boolean valueToReturn) {
        hasUsedFallbackWhereServerDidntTranspondSettings = true;
        return valueToReturn;
    }

    @Override
    public void tick() {
        super.tick();
        if (debugButton != null) {
            debugButton.setMessage(getDebugButtonText());
        }
    }

    private static Component getDebugButtonText() {
        if (DebugLogger.isEnabled()) {
            long secs = DebugLogger.remainingSeconds();
            long mins = secs / 60;
            long remainSecs = secs % 60;
            String timeStr = mins > 0
                    ? String.format("%dm %02ds", mins, remainSecs)
                    : String.format("%ds", remainSecs);
            return Component.translatable("armorhider.options.debug.enabled", timeStr);
        }
        return Component.translatable("armorhider.options.debug.enable");
    }
}
