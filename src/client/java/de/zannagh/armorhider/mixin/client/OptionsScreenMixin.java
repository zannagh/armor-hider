//? if <= 1.21.1 {
/*package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.client.ArmorHiderOptionsScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
//? if < 1.21
//import net.minecraft.client.gui.screens.OptionsScreen;
//? if >= 1.21.1 {

import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
 //?}
import net.minecraft.client.gui.screens.Screen;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;


@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    @Final
    @Shadow
    private Options options;

    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (this.minecraft == null) {
            return;
        }

        // Add Armor Hider button in the bottom left corner
        int buttonWidth = 98;
        int buttonHeight = 20;
        int margin = 5;

        this.addRenderableWidget(Button.builder(
            Component.translatable("armorhider.options.button"),
            button -> this.minecraft.setScreen(new ArmorHiderOptionsScreen(this, this.options))
        ).bounds(margin, this.height - buttonHeight - margin, buttonWidth, buttonHeight).build());
    }
}
*///?}
