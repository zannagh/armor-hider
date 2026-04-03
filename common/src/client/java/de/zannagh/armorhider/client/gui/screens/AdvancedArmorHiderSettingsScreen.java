package de.zannagh.armorhider.client.gui.screens;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.elements.OptionElementFactory;
import de.zannagh.armorhider.client.gui.elements.WidgetList;
import de.zannagh.armorhider.log.DebugLogger;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class AdvancedArmorHiderSettingsScreen extends Screen {

    private final Screen parent;
    private final Options gameOptions;
    private boolean hasUsedFallbackWhereServerDidntTranspondSettings = false;
    private boolean serverSettingsChanged;
    private boolean newServerCombatDetection;
    private boolean setForceArmorHiderOff;
    private boolean localSettingsChanged;
    private boolean setDisableOthers = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHiderForOthers.getValue();
    private boolean setUseLocalSettingsForOthersWhenUnknown = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().usePlayerSettingsWhenUndeterminable.getValue();
    private boolean setDisableLocal = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.getValue();
    private boolean forceServerOffDefaultSetting;
    private boolean combatDetectionDefaultSetting;
    private OptionInstance<Boolean> forceOffOption;
    private OptionInstance<Boolean> combatDetectionServerOption;
    private Button debugButton;

    public AdvancedArmorHiderSettingsScreen(Screen parent, Options gameOptions, Component title) {
        super(title);
        this.parent = parent;
        this.gameOptions = gameOptions;
    }

    @Override
    protected void init() {
        int topMargin = 32;
        int bottomMargin = 32;
        int itemHeight = 25;

        var list = new WidgetList(this.minecraft, this.width, this.height - topMargin - bottomMargin, topMargin, itemHeight);
        int rowWidth = list.getRowWidth();

        OptionElementFactory factory = new OptionElementFactory(list::addWidget, gameOptions, rowWidth);

        addOptionsContent(factory, rowWidth);
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    private void addOptionsContent(OptionElementFactory factory, int rowWidth) {
        factory.addTextWidget(Component.translatable("armorhider.options.admin.title"));

        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        combatDetectionDefaultSetting = serverConfig != null
                && serverConfig.serverWideSettings != null
                && serverConfig.serverWideSettings.enableCombatDetection != null
                ? serverConfig.serverWideSettings.enableCombatDetection.getValue()
                : getFallbackDefault(true);

        forceServerOffDefaultSetting = serverConfig != null
                && serverConfig.serverWideSettings != null
                && serverConfig.serverWideSettings.forceArmorHiderOff != null
                ? serverConfig.serverWideSettings.forceArmorHiderOff.getValue()
                : getFallbackDefault(false);

        var combatDetectionServerText = Component.translatable("armorhider.options.combat_detection_server.title");
        var forceArmorHiderOffText = Component.translatable("armorhider.options.force_armor_hider_off.title");

        //? if >= 1.21.9 {
        var onText = Component.translatable("armorhider.options.toggle.on");
        var offText = Component.translatable("armorhider.options.toggle.off");

        //? if >= 1.21.11
        var cyclingWidgetBuilder = CycleButton.booleanBuilder(onText, offText, combatDetectionDefaultSetting);
        //? if >= 1.21.9 && < 1.21.11
        //var cyclingWidgetBuilder = CycleButton.booleanBuilder(onText, offText).withInitialValue(combatDetectionDefaultSetting);
        //? if >= 1.21.11
        var forceOnOffBuilder = CycleButton.booleanBuilder(onText, offText, forceServerOffDefaultSetting);
        //? if >= 1.21.9 && < 1.21.11
        //var forceOnOffBuilder = CycleButton.booleanBuilder(onText, offText).withInitialValue(forceServerOffDefaultSetting);

        var cyclingWidget = cyclingWidgetBuilder.withTooltip(newValue -> {
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
                    setServerCombatDetection(newValue);
                }
        );

        var armorHiderOffWidget = forceOnOffBuilder.withTooltip(newValue -> {
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
                    setForceArmorHiderOff(newValue);
                }
        );

        armorHiderOffWidget.active = ArmorHiderClient.permissionLevel >= 3;
        cyclingWidget.active = ArmorHiderClient.permissionLevel >= 3;

        factory.addElementAsWidget(cyclingWidget);
        factory.addElementAsWidget(armorHiderOffWidget);
        //?}

        //? if < 1.21.9 {
        /*combatDetectionServerOption = factory.buildBooleanOption(
                combatDetectionServerText,
                ArmorHiderClient.permissionLevel >= 3
                        ? Component.translatable("armorhider.options.combat_detection_server.tooltip")
                        : Component.translatable("armorhider.options.combat_detection_server.tooltip.disabled"),
                null,
                combatDetectionDefaultSetting,
                this::setServerCombatDetection
        );

        forceOffOption = factory.buildBooleanOption(
                forceArmorHiderOffText,
                ArmorHiderClient.permissionLevel >= 3
                        ? Component.translatable("armorhider.options.force_armor_hider_off.tooltip")
                        : Component.translatable("armorhider.options.force_armor_hider_off.tooltip.disabled"),
                null,
                forceServerOffDefaultSetting,
                this::setForceArmorHiderOff
        );

        var combatBtn = combatDetectionServerOption.createButton(gameOptions, 0, 0, rowWidth);
        combatBtn.active = ArmorHiderClient.permissionLevel >= 3;
        factory.addElementAsWidget(combatBtn);

        var forceOffBtn = forceOffOption.createButton(gameOptions, 0, 0, rowWidth);
        forceOffBtn.active = ArmorHiderClient.permissionLevel >= 3;
        factory.addElementAsWidget(forceOffBtn);
        *///?}

        factory.addTextWidget(Component.translatable("armorhider.options.regular.title"));

        var settingsToUse = factory.buildBooleanOption(
                Component.translatable("armorhider.options.use_settings_for_unknown.title"),
                Component.translatable("armorhider.options.use_settings_for_unknown.tooltip"),
                Component.translatable("armorhider.options.use_settings_for_unknown.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().usePlayerSettingsWhenUndeterminable.getValue(),
                this::setUseLocalSettingsForOthersWhenUnknown
        );

        var globalToggle = factory.buildBooleanOption(
                Component.translatable("armorhider.options.disable_local.title"),
                Component.translatable("armorhider.options.disable_local.tooltip"),
                Component.translatable("armorhider.options.disable_local.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.getValue(),
                this::setDisableLocal
        );

        var otherToggle = factory.buildBooleanOption(
                Component.translatable("armorhider.options.disable_others.title"),
                Component.translatable("armorhider.options.disable_others.tooltip"),
                Component.translatable("armorhider.options.disable_others.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHiderForOthers.getValue(),
                this::setDisableOthers
        );

        factory.addSimpleOptionAsWidget(settingsToUse);
        factory.addSimpleOptionAsWidget(globalToggle);
        factory.addSimpleOptionAsWidget(otherToggle);

        factory.addTextWidget(Component.translatable("armorhider.options.debug.title"));

        debugButton = Button.builder(
                getDebugButtonText(),
                btn -> {
                    if (DebugLogger.isEnabled()) {
                        DebugLogger.disable();
                    } else {
                        DebugLogger.enable();
                    }
                    btn.setMessage(getDebugButtonText());
                })
                .tooltip(Tooltip.create(Component.translatable("armorhider.options.debug.tooltip")))
                .build();
        factory.addElementAsWidget(debugButton);
    }

    @Override
    public void onClose() {
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
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private boolean getFallbackDefault(boolean valueToReturn) {
        hasUsedFallbackWhereServerDidntTranspondSettings = true;
        return valueToReturn;
    }

    private void setServerCombatDetection(boolean enabled) {
        if (ArmorHiderClient.permissionLevel < 3) {
            if (combatDetectionServerOption != null) {
                combatDetectionServerOption.set(combatDetectionDefaultSetting);
            }
            return;
        }
        newServerCombatDetection = enabled;
        serverSettingsChanged = true;
    }

    private void setForceArmorHiderOff(boolean enabled) {
        if (ArmorHiderClient.permissionLevel < 3) {
            if (forceOffOption != null) {
                forceOffOption.set(forceServerOffDefaultSetting);
            }
            return;
        }
        setForceArmorHiderOff = enabled;
        serverSettingsChanged = true;
    }

    private void setDisableOthers(boolean enabled) {
        setDisableOthers = enabled;
        localSettingsChanged = true;
    }

    private void setUseLocalSettingsForOthersWhenUnknown(boolean enabled) {
        setUseLocalSettingsForOthersWhenUnknown = enabled;
        localSettingsChanged = true;
    }

    private void setDisableLocal(boolean enabled) {
        setDisableLocal = enabled;
        localSettingsChanged = true;
    }

    //? if < 1.21.4 {
    /*@Override
    public void render(net.minecraft.client.gui.GuiGraphics context, int mouseX, int mouseY, float delta) {
        //? if >= 1.21
        //this.renderBackground(context, mouseX, mouseY, delta);
        //? if < 1.21 {
        /^this.renderBackground(context);
        ^///?}
        super.render(context, mouseX, mouseY, delta);
    }
    *///?}

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
