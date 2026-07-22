package de.zannagh.armorhider.client.gui.elements;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.elements.factories.OptionElementFactory;
import de.zannagh.armorhider.client.gui.elements.implementations.AccessoryAffectButton;
import de.zannagh.armorhider.client.gui.elements.implementations.ShowShieldWhenBlockingButton;
import de.zannagh.armorhider.client.gui.screens.AdvancedArmorHiderSettingsScreen;
import de.zannagh.armorhider.configuration.PresetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
//? if > 1.21.8 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.NonNull;
import org.jetbrains.annotations.Nullable;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.function.Consumer;

public class ArmorHiderOptionsPanelWidget extends AbstractWidget {
    private static final int ITEM_HEIGHT = 25;

    private final Screen hostScreen;
    private final Options gameOptions;
    private final Runnable onDirty;
    private final PresetManager presetManager;
    public final WidgetList widgetList;

    /**
     * When non-null the panel edits this specific config (a per-player override) instead of the local
     * player's own config.
     */
    private @Nullable PlayerConfig targetConfig;

    /** Whether to show the global presets + the "advanced" navigation button (only for the local config). */
    private final boolean showPresets;

    public ArmorHiderOptionsPanelWidget(int x, int y, int width, int height, Screen hostScreen, Options gameOptions, Runnable onDirty, PresetManager presetManager) {
        this(x, y, width, height, hostScreen, gameOptions, onDirty, presetManager, null, true);
    }

    public ArmorHiderOptionsPanelWidget(int x, int y, int width, int height, Screen hostScreen, Options gameOptions, Runnable onDirty, PresetManager presetManager, @Nullable PlayerConfig targetConfig, boolean showPresets) {
        super(x, y, width, height, Component.translatable("armorhider.options.mod_title"));
        this.hostScreen = hostScreen;
        this.gameOptions = gameOptions;
        this.onDirty = onDirty;
        this.presetManager = presetManager;
        this.targetConfig = targetConfig;
        this.showPresets = showPresets;
        this.widgetList = new WidgetList(Minecraft.getInstance(), width, height, y, ITEM_HEIGHT);

        populateOptions();
        updateLayout();
    }

    /** Returns the config this panel edits: an explicit per-player target, or the local player's own config. */
    private PlayerConfig configSource() {
        return targetConfig != null ? targetConfig : ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig();
    }

    /** Rebinds the panel to a different config (used when the selected player changes) and rebuilds it. */
    public void bindConfig(@Nullable PlayerConfig config) {
        this.targetConfig = config;
        rebuildOptions();
    }

    /**
     * The per-slot accessory-hide toggle for a slot, or {@code null} when no accessory provider is
     * loaded (so the button is omitted from the row entirely).
     */
    private @Nullable AbstractWidget accessoryButtonFor(EquipmentSlot slot, boolean initial, Consumer<Boolean> setter) {
        if (!CompatManager.anyAccessoryProviderLoaded()) {
            return null;
        }
        return new AccessoryAffectButton(initial, slot, UiConstants.SQUARE_BUTTON_WIDTH, UiConstants.DEFAULT_BUTTON_HEIGHT,
                onPress -> {
                    if (onPress instanceof AccessoryAffectButton btn) {
                        setSetting(btn.toggle(), setter);
                    }
                });
    }

    private void populateOptions() {
        var factory = new OptionElementFactory(widgetList::addWidget, gameOptions, widgetList.getRowWidth());
        var config = configSource();

        ArrayList<Pair<Boolean, Consumer<Boolean>>> configs = new ArrayList<>();
        configs.add(new Pair<>(config.enableCombatDetection.getValue(), val -> setSetting(val, config.enableCombatDetection::setValue)));
        configs.add(new Pair<>(config.inCombatUseDefaultModel.getValue(), val -> setSetting(val, config.inCombatUseDefaultModel::setValue)));
        configs.add(new Pair<>(config.disableArmorHiderOnInvisibility.getValue(), val -> setSetting(val, config.disableArmorHiderOnInvisibility::setValue)));
        // Master accessory-hide toggle — only offered (as a 4th general-row button) when an accessory
        // provider (Curios / Trinkets / Artifacts) is present, so vanilla users don't see a dead toggle.
        if (CompatManager.anyAccessoryProviderLoaded()) {
            configs.add(new Pair<>(config.affectAccessories.getValue(), val -> setSetting(val, config.affectAccessories::setValue)));
        }

        if (showPresets) {
            // Local config: general behaviour toggles + presets + the "individual settings" entry, one row.
            factory.addElementAsWidget(factory.createCompoundButtonWidget(
                    configs, presetManager, presetManager.getActiveIndex(), this::onPresetActivated
            ));
        } else {
            // Per-player override: behaviour toggles only (presets/individual-settings don't apply here).
            factory.addElementAsWidget(factory.createGeneralTogglesRow(configs));
        }

        var helmetOption = factory.buildDoubleOption(
                "armorhider.helmet.transparency",
                Component.translatable("armorhider.options.helmet.tooltip"),
                Component.translatable("armorhider.options.helmet.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.helmet.button_text", String.format("%.0f%%", currentValue * 100)),
                config.helmetOpacity.getValue(),
                val -> setSetting(val, config.helmetOpacity::setValue)
        );
        factory.addSliderWithToggles(
                EquipmentSlot.HEAD,
                helmetOption,
                gameOptions,
                config.helmetGlint.getValue(),
                config.opacityAffectingHatOrSkull.getValue(),
                val -> setSetting(val, config.helmetGlint::setValue),
                val -> setSetting(val, config.opacityAffectingHatOrSkull::setValue),
                null,
                accessoryButtonFor(EquipmentSlot.HEAD, config.affectHeadAccessory.getValue(), config.affectHeadAccessory::setValue)
        );

        var chestOption = factory.buildDoubleOption(
                "armorhider.chestplate.transparency",
                Component.translatable("armorhider.options.chestplate.tooltip"),
                Component.translatable("armorhider.options.chestplate.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.chestplate.button_text", String.format("%.0f%%", currentValue * 100)),
                config.chestOpacity.getValue(),
                val -> setSetting(val, config.chestOpacity::setValue)
        );
        factory.addSliderWithToggles(
                EquipmentSlot.CHEST,
                chestOption,
                gameOptions,
                config.chestGlint.getValue(),
                config.opacityAffectingElytra.getValue(),
                val -> setSetting(val, config.chestGlint::setValue),
                val -> setSetting(val, config.opacityAffectingElytra::setValue),
                null,
                accessoryButtonFor(EquipmentSlot.CHEST, config.affectChestAccessory.getValue(), config.affectChestAccessory::setValue)
        );

        var legsOption = factory.buildDoubleOption(
                "armorhider.legs.transparency",
                Component.translatable("armorhider.options.leggings.tooltip"),
                Component.translatable("armorhider.options.leggings.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.leggings.button_text", String.format("%.0f%%", currentValue * 100)),
                config.legsOpacity.getValue(),
                val -> setSetting(val, config.legsOpacity::setValue)
        );
        factory.addSliderWithToggles(
                EquipmentSlot.LEGS,
                legsOption,
                gameOptions,
                config.legsGlint.getValue(),
                null,
                val -> setSetting(val, config.legsGlint::setValue),
                null,
                null,
                accessoryButtonFor(EquipmentSlot.LEGS, config.affectLegsAccessory.getValue(), config.affectLegsAccessory::setValue)
        );

        var bootsOption = factory.buildDoubleOption(
                "armorhider.boots.transparency",
                Component.translatable("armorhider.options.boots.tooltip"),
                Component.translatable("armorhider.options.boots.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.boots.button_text", String.format("%.0f%%", currentValue * 100)),
                config.bootsOpacity.getValue(),
                val -> setSetting(val, config.bootsOpacity::setValue)
        );
        factory.addSliderWithToggles(
                EquipmentSlot.FEET,
                bootsOption,
                gameOptions,
                config.bootsGlint.getValue(),
                null,
                val -> setSetting(val, config.bootsGlint::setValue),
                null,
                null,
                accessoryButtonFor(EquipmentSlot.FEET, config.affectFeetAccessory.getValue(), config.affectFeetAccessory::setValue)
        );

        var offhandOption = factory.buildDoubleOption(
                "armorhider.offhand.transparency",
                Component.translatable("armorhider.options.offhand.tooltip"),
                Component.translatable("armorhider.options.offhand.tooltip_narration"),
                currentValue -> Component.translatable("armorhider.options.offhand.button_text", String.format("%.0f%%", currentValue * 100)),
                config.offHandOpacity.getValue(),
                val -> setSetting(val, config.offHandOpacity::setValue)
        );
        var shieldButton = new ShowShieldWhenBlockingButton(
                config.showShieldWhenBlocking.getValue(),
                UiConstants.SQUARE_BUTTON_WIDTH,
                UiConstants.DEFAULT_BUTTON_HEIGHT,
                onPress -> {
                    if (onPress instanceof ShowShieldWhenBlockingButton btn) {
                        setSetting(btn.toggle(), config.showShieldWhenBlocking::setValue);
                    }
                });
        factory.addSliderWithToggles(
                EquipmentSlot.OFFHAND,
                offhandOption,
                gameOptions,
                null,
                null,
                null,
                null,
                shieldButton
        );

        if (showPresets) {
            factory.addElementAsWidget(Button.builder(
                    Component.translatable("armorhider.options.regular.title"),
                    btn -> Minecraft.getInstance().setScreenAndShow(new AdvancedArmorHiderSettingsScreen(this.hostScreen, this.gameOptions, this.hostScreen.getTitle()))
            ).tooltip(Tooltip.create(Component.translatable("armorhider.options.regular.title"))).build());
        }
    }

    public int getContentHeight() {
        return this.widgetList.children().size() * this.widgetList.getRowHeight();
    }

    private void onPresetActivated(int presetIndex) {
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig();
        if (presetManager.isActive(presetIndex)) {
            presetManager.deactivate();
            rebuildOptions();
            return;
        }
        if (presetManager.hasPreset(presetIndex)) {
            presetManager.setActiveIndex(presetIndex);
            presetManager.getPreset(presetIndex).applyTo(config);
            onDirty.run();
            rebuildOptions();
        } else {
            presetManager.saveFromCurrentConfig(presetIndex, config);
            presetManager.setActiveIndex(presetIndex);
            rebuildOptions();
        }
    }

    private void rebuildOptions() {
        widgetList.clearWidgets();
        populateOptions();
        updateLayout();
    }

    private <T> void setSetting(T value, Consumer<T> setter) {
        setter.accept(value);
        // Per-player overrides are independent of the user's own presets; only sync presets for the local config.
        if (targetConfig == null) {
            presetManager.updateActivePreset(ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig());
        }
        onDirty.run();
    }

    private void updateLayout() {
        //? if > 1.21.8
        widgetList.updateSizeAndPosition(this.width, this.height, this.getX(), this.getY());
        //? if >= 1.21 && <= 1.21.8
        //widgetList.updateSizeAndPosition(this.width, this.height, this.getY());
        //? if < 1.21
        //widgetList.updateSizeAndPosition(this.width, this.height, this.getX(), this.getY());
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateLayout();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateLayout();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        updateLayout();
    }

    //? if >= 1.21 {
    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        updateLayout();
    }
    //?}
    //? if < 1.21 {
    /*public void setHeight(int height) {
        this.height = height;
        updateLayout();
    }
    *///?}

    @Override
    //? if >= 26.1-1.pre.1 {
    /*protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        widgetList.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }
    *///?}
    //? if >= 1.21 && < 26.1-1.pre.1 {
    protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        widgetList.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    //?}
    //? if < 1.21 {
    /*public void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        widgetList.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    *///?}

    @Override
    //? if > 1.21.8
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
    //? if <= 1.21.8
    //public boolean mouseClicked(double d, double e, int i) {
        if (!this.active || !this.visible) {
            return false;
        }
        //? if > 1.21.8
        return widgetList.mouseClicked(event, doubleClick);
        //? if <= 1.21.8
        //return widgetList.mouseClicked(d, e, i);
    }

    @Override
    //? if > 1.21.8
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
    //? if <= 1.21.8
    //public boolean mouseReleased(double d, double e, int i) {
        //? if > 1.21.8
        return widgetList.mouseReleased(event);
        //? if <= 1.21.8
        //return widgetList.mouseReleased(d, e, i);
    }

    @Override
    //? if > 1.21.8
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
    //? if <= 1.21.8
    //public boolean mouseDragged(double d, double e, int i, double f, double g) {
        //? if > 1.21.8
        return widgetList.mouseDragged(event, dx, dy);
        //? if <= 1.21.8
        //return widgetList.mouseDragged(d, e, i, f, g);
    }

    @Override
    //? if >= 1.21 {
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return widgetList.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    //?}
    //? if < 1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return widgetList.mouseScrolled(mouseX, mouseY, scrollY);
    }
    *///?}

    //? if > 1.21.8 {
    @Override
    public boolean keyPressed(KeyEvent event) {
        return widgetList.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return widgetList.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return widgetList.charTyped(event);
    }
    //?}
    //? if <= 1.21.8 {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return widgetList.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return widgetList.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return widgetList.charTyped(chr, modifiers);
    }
    *///?}

    //? if >= 1.21.9 {
    @Override
    public void visitWidgets(Consumer<AbstractWidget> consumer) {
        consumer.accept(widgetList);
    }
    //?}

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
        widgetList.updateNarration(output);
    }
}
