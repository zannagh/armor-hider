package de.zannagh.armorhider.gui;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.OptionElementFactory;
import de.zannagh.armorhider.rendering.RenderUtilities;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
//? if >= 1.21 {
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
//?}
//? if < 1.21 {
/*import net.minecraft.client.gui.screens.OptionsSubScreen;
*///?}
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.Component;

public class AdvancedArmorHiderSettingsScreen extends OptionsSubScreen {

    private boolean hasUsedFallbackWhereServerDidntTranspondSettings = false;

    private boolean serverSettingsChanged;

    private boolean newServerCombatDetection;

    private boolean setForceArmorHiderOff;

    private boolean localSettingsChanged;

    private boolean setDisableOthers;

    private boolean setUseLocalSettingsForOthersWhenUnknown;

    private boolean setDisableLocal;

    //? if < 1.21.9 {
    /*protected OptionsList list;
    *///?}

    public AdvancedArmorHiderSettingsScreen(net.minecraft.client.gui.screens.Screen parent, Options gameOptions, Component title) {
        super(parent, gameOptions, title);
    }

    //? if >= 1.21.9 {
    @Override
    protected void addOptions() {
        addOptionsContent();
    }
    //?}

    //? if >= 1.21 && < 1.21.9 {
    /*@Override
    protected void addOptions() {
        addOptionsContent();
    }

    @Override
    protected void init() {
        this.list = new OptionsList(this.minecraft, this.width, this);
        super.init();
        this.addWidget(this.list);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        this.list.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, this.title, this.width / 2, 5, 16777215);
        super.render(graphics, mouseX, mouseY, delta);
    }
    *///?}

    //? if < 1.21 {
    /*@Override
    protected void init() {
        this.list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
        addOptionsContent();
        this.addWidget(this.list);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        this.list.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, this.title, this.width / 2, 5, 16777215);
        super.render(graphics, mouseX, mouseY, delta);
    }
    *///?}

    private void addOptionsContent() {
        OptionElementFactory optionElementFactory = new OptionElementFactory(this, list, options);
        //? if >= 1.21 && < 1.21.9 {
        /*optionElementFactory = optionElementFactory.withWidgetAdder(this::addRenderableWidget);
        *///?}

        //? if >= 1.21.9 {
        var adminCategory = new MultiLineTextWidget(RenderUtilities.getRowWidth(list), 20, Component.translatable("armorhider.options.admin.title"), this.getFont());
        //?}
        //? if < 1.21.9 {
        /*var adminCategory = new MultiLineTextWidget(RenderUtilities.getRowWidth(list), 20, Component.translatable("armorhider.options.admin.title"), net.minecraft.client.Minecraft.getInstance().font);
        *///?}
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

        var onText = Component.translatable("armorhider.options.toggle.on");
        var offText = Component.translatable("armorhider.options.toggle.off");
        var combatDetectionServerText = Component.translatable("armorhider.options.combat_detection_server.title");
        var forceArmorHiderOffText = Component.translatable("armorhider.options.force_armor_hider_off.title");

        //? if >= 1.21.11 {
        var cyclingWidgetBuilder = CycleButton.booleanBuilder(onText, offText, serverCombatDetectionValue);
        var forceOnOffBuilder = CycleButton.booleanBuilder(onText, offText, serverForcingArmorHiderOffValue);
        //?}
        //? if >= 1.20.5 && < 1.21.11 {
        /*var cyclingWidgetBuilder = CycleButton.booleanBuilder(onText, offText).withInitialValue(serverCombatDetectionValue);
        var forceOnOffBuilder = CycleButton.booleanBuilder(onText, offText).withInitialValue(serverForcingArmorHiderOffValue);
        *///?}
        //? if < 1.20.5 {
        /*var cyclingWidgetBuilder = CycleButton.booleanBuilder(onText, offText).withInitialValue(serverCombatDetectionValue);
        var forceOnOffBuilder = CycleButton.booleanBuilder(onText, offText).withInitialValue(serverForcingArmorHiderOffValue);
        *///?}

        int rowWidth = RenderUtilities.getRowWidth(list);
        int rowLeft = RenderUtilities.getRowLeft(list);
        int nextY = RenderUtilities.getNextY(list);

        //? if >= 1.21.9 {
        var cyclingWidget = cyclingWidgetBuilder.withTooltip(newValue -> {
            if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                return Tooltip.create(Component.translatable("armorhider.options.combat_detection_server.tooltip.disabled"));
            }
            return Tooltip.create(Component.translatable("armorhider.options.combat_detection_server.tooltip"));
        }).create(
                combatDetectionServerText,
                (widget, newValue) -> {
                    if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                        widget.setValue(serverCombatDetectionValue);
                        return;
                    }
                    setServerCombatDetection(newValue);
                }
        );

        var armorHiderOffWidget = forceOnOffBuilder.withTooltip(newValue -> {
            if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                return Tooltip.create(Component.translatable("armorhider.options.force_armor_hider_off.tooltip.disabled"));
            }
            return Tooltip.create(Component.translatable("armorhider.options.force_armor_hider_off.tooltip"));
        }).create(
                forceArmorHiderOffText,
                (widget, newValue) -> {
                    if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                        widget.setValue(serverForcingArmorHiderOffValue);
                        return;
                    }
                    setForceArmorHiderOff(newValue);
                }
        );
        //?}
        //? if < 1.21.9 {
        /*var cyclingWidget = cyclingWidgetBuilder.withTooltip(newValue -> {
            if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                return Tooltip.create(Component.translatable("armorhider.options.combat_detection_server.tooltip.disabled"));
            }
            return Tooltip.create(Component.translatable("armorhider.options.combat_detection_server.tooltip"));
        }).create(
                rowLeft, nextY, rowWidth, 20,
                combatDetectionServerText,
                (widget, newValue) -> {
                    if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                        widget.setValue(serverCombatDetectionValue);
                        return;
                    }
                    setServerCombatDetection(newValue);
                }
        );

        var armorHiderOffWidget = forceOnOffBuilder.withTooltip(newValue -> {
            if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                return Tooltip.create(Component.translatable("armorhider.options.force_armor_hider_off.tooltip.disabled"));
            }
            return Tooltip.create(Component.translatable("armorhider.options.force_armor_hider_off.tooltip"));
        }).create(
                rowLeft, nextY + 25, rowWidth, 20,
                forceArmorHiderOffText,
                (widget, newValue) -> {
                    if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
                        widget.setValue(serverForcingArmorHiderOffValue);
                        return;
                    }
                    setForceArmorHiderOff(newValue);
                }
        );
        *///?}

        if (!ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            armorHiderOffWidget.active = false;
            cyclingWidget.active = false;
        }

        //? if >= 1.21.9 {
        cyclingWidget.setSize(RenderUtilities.getRowWidth(list), 20);
        armorHiderOffWidget.setSize(RenderUtilities.getRowWidth(list), 20);
        //?}

        optionElementFactory.addElementAsWidget(cyclingWidget);
        optionElementFactory.addElementAsWidget(armorHiderOffWidget);

        //? if >= 1.21.9 {
        var regularCategory = new MultiLineTextWidget(RenderUtilities.getRowWidth(list), 20, Component.translatable("armorhider.options.regular.title"), this.getFont());
        //?}
        //? if < 1.21.9 {
        /*var regularCategory = new MultiLineTextWidget(RenderUtilities.getRowWidth(list), 20, Component.translatable("armorhider.options.regular.title"), net.minecraft.client.Minecraft.getInstance().font);
        *///?}
        optionElementFactory.addElementAsWidget(regularCategory);

        var settingsToUse = optionElementFactory.buildBooleanOption(
                Component.translatable("armorhider.options.use_settings_for_unknown.title"),
                Component.translatable("armorhider.options.use_settings_for_unknown.tooltip"),
                Component.translatable("armorhider.options.use_settings_for_unknown.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().usePlayerSettingsWhenUndeterminable.getValue(),
                this::setUseLocalSettingsForOthersWhenUnknown
        );

        var globalToggle = optionElementFactory.buildBooleanOption(
                Component.translatable("armorhider.options.disable_local.title"),
                Component.translatable("armorhider.options.disable_local.tooltip"),
                Component.translatable("armorhider.options.disable_local.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.getValue(),
                this::setDisableLocal
        );

        var otherToggle = optionElementFactory.buildBooleanOption(
                Component.translatable("armorhider.options.disable_others.title"),
                Component.translatable("armorhider.options.disable_others.tooltip"),
                Component.translatable("armorhider.options.disable_others.tooltip_narration"),
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHiderForOthers.getValue(),
                this::setDisableOthers
        );

        optionElementFactory.addSimpleOptionAsWidget(settingsToUse);
        optionElementFactory.addSimpleOptionAsWidget(globalToggle);
        optionElementFactory.addSimpleOptionAsWidget(otherToggle);
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
        super.onClose();
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
}
