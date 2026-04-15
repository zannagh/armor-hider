package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.ArmorHiderClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

//region Conditional Imports
//? if < 1.21 {
/*
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.OptionsScreen;
*/
//?} else {
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.options.OptionsScreen;
//?}
//endregion

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    @Final
    @Shadow
    private Options options;

    protected OptionsScreenMixin(Component title) {
        super(title);
    }
    
    //? if < 1.21 {
    /*
    @WrapOperation(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;I)Lnet/minecraft/client/gui/layouts/LayoutElement;"
            )
    )
    private <T extends LayoutElement> T interceptSpacer(GridLayout.RowHelper instance, T layoutElement, int i, Operation<T> original){
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().showSettingsInSkinCustomization.getValue()) {
            return original.call(instance, layoutElement, i);
        }
        if (layoutElement instanceof SpacerElement) {
            original.call(instance, layoutElement, i);
        }
        var settingsButton = Button.builder(
                Component.translatable("armorhider.options.mod_title"),
                button ->
                        //? if < 1.21
                        //ArmorHiderClient.openPreferredSettingsScreen(this, this.options)
                //? if >= 1.21
                ArmorHiderClient.openPreferredSettingsScreen(this, this.options)
        ).width(200).build();
        instance.addChild(settingsButton, 2);
        return layoutElement;
    }
    *///?}
    
    //? if >= 1.21{
    @WrapOperation(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout;createRowHelper(I)Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;"
            )
    )
    private GridLayout.RowHelper interceptSpacer(GridLayout instance, int i, Operation<GridLayout.RowHelper> original) {
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().showSettingsInSkinCustomization.getValue()) {
            return original.call(instance, i);
        }
        var returnValue = original.call(instance, i);
        var settingsButton = Button.builder(
                Component.translatable("armorhider.options.mod_title"),
                button ->
                        //? if < 1.21
                        //ArmorHiderClient.openPreferredSettingsScreen(this, this.options)
                        //? if >= 1.21
                        ArmorHiderClient.openPreferredSettingsScreen(this, this.options)).width(200).build();
        returnValue.addChild(settingsButton, 2);
        return returnValue;
    }
    //?}
}
