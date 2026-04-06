package de.zannagh.armorhider.client.gui.screens;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.configuration.ExclusionItemConfiguration;
import de.zannagh.armorhider.configuration.ExclusionItemInfo;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Screen that shows a scrollable list of items for a given equipment slot,
 * each with a toggle to enable/disable the mod's handling of that item.
 * <p>
 * Toggle ON (green) = mod intercepts and can make the item transparent.
 * Toggle OFF (red) = mod ignores the item, it always renders normally.
 */
public class ItemExclusionScreen extends ArmorHiderConfigurationScreen {

    private final EquipmentSlot slot;

    public ItemExclusionScreen(Screen parent, Options gameOptions, EquipmentSlot slot) {
        super(parent, gameOptions, Component.translatable("armorhider.options.item_exclusion.title",
                Component.translatable("armorhider.slot." + slot.name().toLowerCase())));
        this.slot = slot;
    }

    @Override
    protected void init() {
        super.initWidgetList(this.width);
        super.init();
    }
    
    @Override
    protected void addOptions() {
        ExclusionItemConfiguration exclusionConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.local().getExclusionItems();
        Map<String, ExclusionItemInfo> slotItems = exclusionConfig.getItemsForSlot(slot);

        for (Map.Entry<String, ExclusionItemInfo> entry : slotItems.entrySet()) {
            String itemId = entry.getKey();
            ExclusionItemInfo info = entry.getValue();

            // Try to get the localized item name; fall back to config display name
            String displayName = getLocalizedItemName(itemId, info.displayName);

            // Initial value: true = mod handles (intercepted), false = mod ignores
            boolean initialValue = !info.shouldIgnore;

            //? if >= 1.21.9 {
            var onText = Component.translatable("armorhider.options.item_exclusion.handled");
            var offText = Component.translatable("armorhider.options.item_exclusion.ignored");

            //? if >= 1.21.11
            var cycleBuilder = net.minecraft.client.gui.components.CycleButton.booleanBuilder(onText, offText, initialValue);
            //? if >= 1.21.9 && < 1.21.11
            //var cycleBuilder = net.minecraft.client.gui.components.CycleButton.booleanBuilder(onText, offText).withInitialValue(initialValue);

            var cycleWidget = cycleBuilder.withTooltip(value -> {
                if (value) {
                    return Tooltip.create(Component.translatable("armorhider.options.item_exclusion.handled.tooltip", displayName));
                }
                return Tooltip.create(Component.translatable("armorhider.options.item_exclusion.ignored.tooltip", displayName));
            }).create(
                    0, 0, rowWidth, 20,
                    Component.literal(displayName),
                    (widget, newValue) -> setSetting(newValue, val -> info.shouldIgnore = !val)
            );
            widgetList.addWidget(cycleWidget);
            //?}

            //? if < 1.21.9 {
            /*var option = net.minecraft.client.OptionInstance.createBoolean(
                    displayName,
                    net.minecraft.client.OptionInstance.noTooltip(),
                    initialValue,
                    newValue -> setSetting(newValue, val -> info.shouldIgnore = !val)
            );
            widgetList.addWidget(option.createButton(net.minecraft.client.Minecraft.getInstance().options, 0, 0, rowWidth));
            *///?}
        }
    }

    @Override
    protected void saveSettingsOnClose() {
        ArmorHider.LOGGER.info("Saving item exclusion changes for slot {}...", slot.name());
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
    }

    private static String getLocalizedItemName(String itemId, String fallback) {
        try {
            Item item = ExclusionItemConfiguration.getItemFromId(itemId);
            String name = new ItemStack(item).getHoverName().getString();
            if (!name.isEmpty() && !name.equals("Air")) {
                return name;
            }
        } catch (Exception e) {
            // Registry lookup failed - use fallback
        }
        return fallback.isEmpty() ? itemId : fallback;
    }

    //? if < 1.21.4 {
    /*@Override
    public void render(net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        //? if >= 1.21
        this.renderBackground(context, mouseX, mouseY, delta);
        //? if < 1.21 {
        /^this.renderBackground(context);
        ^///?}
        super.render(context, mouseX, mouseY, delta);
    }
    *///?}
}
