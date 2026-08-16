package de.zannagh.armorhider.client.gui.screens;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.configuration.IrisPartialTransparencyMode;
import de.zannagh.armorhider.configuration.SettingsLocation;
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
    private SettingsLocation setSettingsLocation = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().settingsScreenLocation.getValue();
    private boolean setDisableLocal = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().disableArmorHider.getValue();
    private boolean forceServerOffDefaultSetting;
    private boolean combatDetectionDefaultSetting;

    private boolean visibilityRespectDefaultSetting;
    private boolean allowIndividualConfigsDefaultSetting;
    private boolean setAllowIndividualConfigs;
    private Button debugButton;

    // Iris partial-transparency (dithering) settings - local, per-client render prefs. Seeded from the
    // saved local config; written back in saveSettingsOnClose. The widgets are only shown where the
    // dithering path exists (26.2 family), but the fields compile everywhere (harmless no-op elsewhere).
    private IrisPartialTransparencyMode setIrisMode = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().irisPartialTransparencyMode.getValue();
    private int setIrisScale = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().irisDitheringScale.getValue();
    private int setIrisPhases = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().irisDitheringPhases.getValue();
    private int setIrisResCap = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig().irisDitheringResCap.getValue();

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
        // Seed every "new value" field to the current server default so that saving after changing only one
        // setting doesn't reset the untouched ones (saveSettingsOnClose sends all four together).
        setAllowIndividualConfigs = allowIndividualConfigsDefaultSetting;
        newServerCombatDetection = combatDetectionDefaultSetting;
        setForceArmorHiderOff = forceServerOffDefaultSetting;

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

        // Three-way cycle: where the Armor Hider settings entry point lives (Skin Customization screen,
        // Options screen, or HIDDEN - reachable only via the keybind). A plain cycling Button rather than
        // a boolean CycleButton so it works uniformly across every version, like the debug button below.
        var settingsLocationButton = Button.builder(
                settingsLocationButtonText(setSettingsLocation),
                btn -> {
                    setSetting(nextSettingsLocation(setSettingsLocation), v -> {
                        setSettingsLocation = v;
                        localSettingsChanged = true;
                    });
                    btn.setMessage(settingsLocationButtonText(setSettingsLocation));
                    btn.setTooltip(Tooltip.create(settingsLocationTooltip(setSettingsLocation)));
                })
                .tooltip(Tooltip.create(settingsLocationTooltip(setSettingsLocation)))
                .build();
        factory.addSimpleOptionAsWidget(globalToggle);
        factory.addElementAsWidget(settingsLocationButton);

        // Iris partial-transparency (dithering) settings. Only shown on the version family where the
        // dithering render path actually runs (26.2); on other versions the render path is a no-op, so
        // exposing knobs there would just be confusing.
        //? if >= 26.2-1.pre && < 26.3-0.snapshot.2 {
        factory.addTextWidget(Component.translatable("armorhider.options.iris_dithering.title"));

        var irisModeButton = Button.builder(
                irisModeButtonText(setIrisMode),
                btn -> {
                    setSetting(nextIrisMode(setIrisMode), v -> {
                        setIrisMode = v;
                        localSettingsChanged = true;
                    });
                    btn.setMessage(irisModeButtonText(setIrisMode));
                    btn.setTooltip(Tooltip.create(irisModeTooltip(setIrisMode)));
                })
                .tooltip(Tooltip.create(irisModeTooltip(setIrisMode)))
                .build();
        factory.addElementAsWidget(irisModeButton);

        factory.addSimpleOptionAsWidget(buildIrisIntSlider(
                "armorhider.options.iris_scale", "armorhider.options.iris_scale.tooltip",
                1, 256, setIrisScale, v -> {
                    setIrisScale = v;
                    localSettingsChanged = true;
                }));
        factory.addSimpleOptionAsWidget(buildIrisIntSlider(
                "armorhider.options.iris_phases", "armorhider.options.iris_phases.tooltip",
                1, 256, setIrisPhases, v -> {
                    setIrisPhases = v;
                    localSettingsChanged = true;
                }));
        factory.addSimpleOptionAsWidget(buildIrisIntSlider(
                "armorhider.options.iris_rescap", "armorhider.options.iris_rescap.tooltip",
                256, 16384, setIrisResCap, v -> {
                    setIrisResCap = v;
                    localSettingsChanged = true;
                }));
        //?}

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
            var localConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig();
            localConfig.disableArmorHider.setValue(setDisableLocal);
            localConfig.settingsScreenLocation.setValue(setSettingsLocation);
            localConfig.irisPartialTransparencyMode.setValue(setIrisMode);
            localConfig.irisDitheringScale.setValue(setIrisScale);
            localConfig.irisDitheringPhases.setValue(setIrisPhases);
            localConfig.irisDitheringResCap.setValue(setIrisResCap);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
        }
    }

    private static SettingsLocation nextSettingsLocation(SettingsLocation current) {
        var values = SettingsLocation.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    private static Component settingsLocationButtonText(SettingsLocation location) {
        return Component.translatable("armorhider.options.settings_location.button", settingsLocationLabel(location));
    }

    private static Component settingsLocationLabel(SettingsLocation location) {
        return switch (location) {
            case SKIN_CUSTOMIZATION -> Component.translatable("armorhider.options.settings_location.skin_customization");
            case OPTIONS_SCREEN -> Component.translatable("armorhider.options.settings_location.options_screen");
            case HIDDEN -> Component.translatable("armorhider.options.settings_location.hidden");
        };
    }

    private static Component settingsLocationTooltip(SettingsLocation location) {
        return switch (location) {
            case SKIN_CUSTOMIZATION -> Component.translatable("armorhider.options.settings_location.tooltip.skin_customization");
            case OPTIONS_SCREEN -> Component.translatable("armorhider.options.settings_location.tooltip.options_screen");
            case HIDDEN -> Component.translatable("armorhider.options.settings_location.tooltip.hidden");
        };
    }

    //? if >= 26.2-1.pre && < 26.3-0.snapshot.2 {
    private static IrisPartialTransparencyMode nextIrisMode(IrisPartialTransparencyMode current) {
        var values = IrisPartialTransparencyMode.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    private static Component irisModeButtonText(IrisPartialTransparencyMode mode) {
        return Component.translatable("armorhider.options.iris_mode.button", irisModeLabel(mode));
    }

    private static Component irisModeLabel(IrisPartialTransparencyMode mode) {
        return switch (mode) {
            case NONE -> Component.translatable("armorhider.options.iris_mode.none");
            case DITHERING -> Component.translatable("armorhider.options.iris_mode.dithering");
            case TEMPORAL_DITHERING -> Component.translatable("armorhider.options.iris_mode.temporal");
        };
    }

    private static Component irisModeTooltip(IrisPartialTransparencyMode mode) {
        return switch (mode) {
            case NONE -> Component.translatable("armorhider.options.iris_mode.tooltip.none");
            case DITHERING -> Component.translatable("armorhider.options.iris_mode.tooltip.dithering");
            case TEMPORAL_DITHERING -> Component.translatable("armorhider.options.iris_mode.tooltip.temporal");
        };
    }

    // Builds a labelled integer slider ("<name>: <value>") for the dithering knobs. applyValueImmediately
    // is false so the listener fires on release, not every drag step - the setter only stores a field
    // here, but keeping it off matches the opacity sliders and avoids needless churn.
    private OptionInstance<Integer> buildIrisIntSlider(String key, String tooltipKey, int min, int max,
                                                       int current, java.util.function.Consumer<Integer> onChange) {
        return new OptionInstance<>(
                key,
                OptionInstance.cachedConstantTooltip(Component.translatable(tooltipKey)),
                (caption, value) -> Component.translatable(key).copy().append(": " + value),
                new OptionInstance.IntRange(min, max, false),
                current,
                value -> setSetting(value, onChange));
    }
    //?}

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
