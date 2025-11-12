package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.ArmorVisibilityWidgets;
import de.zannagh.armorhider.ClientConfigManager;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.SkinOptionsScreen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptionsScreen.class)
public abstract class SkinOptionsMixin {

    @Shadow
    protected OptionListWidget body;

    @Inject(method = "init", at = @At("RETURN"))
    private void onAddOptions(CallbackInfo ci) {
        var current = ((GameOptionsScreen)(Object)this);
        if (!(current instanceof SkinOptionsScreen)) {
            return;
        }

        if (body == null) {
            return;
        }

        // Clear any previous widget references
        ArmorVisibilityWidgets.clear();

        // Add "Toggle All" button
        body.addWidgetEntry(
                CyclingButtonWidget.onOffBuilder()
                        .initially(!ClientConfigManager.isEnabled())
                        .build(0, 0, 150, 20, Text.literal("Toggle All Armor"), (button, value) -> {
                            ClientConfigManager.setEnabled(value);
                            ArmorVisibilityWidgets.updateAllButtons(value);
                        }),
                null
        );

        // Add individual armor piece toggles
        CyclingButtonWidget<Boolean> helmetButton = CyclingButtonWidget.onOffBuilder()
                .initially(ClientConfigManager.get().helmetTransparency >= 0.95)
                .build(0, 0, 150, 20, Text.literal("Helmet Visibility"), (button, value) -> {
                    ClientConfigManager.setHelmet(value);
                });
        ArmorVisibilityWidgets.setHelmetButton(helmetButton);

        CyclingButtonWidget<Boolean> chestButton = CyclingButtonWidget.onOffBuilder()
                .initially(ClientConfigManager.get().chestTransparency >= 0.95)
                .build(0, 0, 150, 20, Text.literal("Chestplate Visibility"), (button, value) -> {
                    ClientConfigManager.setChest(value);
                });
        ArmorVisibilityWidgets.setChestButton(chestButton);

        body.addWidgetEntry(helmetButton, chestButton);

        CyclingButtonWidget<Boolean> legsButton = CyclingButtonWidget.onOffBuilder()
                .initially(ClientConfigManager.get().legsTransparency >= 0.95)
                .build(0, 0, 150, 20, Text.literal("Leggings Visibility"), (button, value) -> {
                    ClientConfigManager.setLegs(value);
                });
        ArmorVisibilityWidgets.setLegsButton(legsButton);

        CyclingButtonWidget<Boolean> bootsButton = CyclingButtonWidget.onOffBuilder()
                .initially(ClientConfigManager.get().bootsTransparency >= 0.95)
                .build(0, 0, 150, 20, Text.literal("Boots Visibility"), (button, value) -> {
                    ClientConfigManager.setBoots(value);
                });
        ArmorVisibilityWidgets.setBootsButton(bootsButton);

        body.addWidgetEntry(legsButton, bootsButton);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        var current = ((GameOptionsScreen)(Object)this);
        if (!(current instanceof SkinOptionsScreen)) {
            return;
        }

        // Clear widget references when screen is closed
        ArmorVisibilityWidgets.clear();
    }
}
