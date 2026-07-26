package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.keybinds.CustomKeyMapping;
import de.zannagh.armorhider.client.keybinds.LoadPresetKeyMapping;
import de.zannagh.armorhider.client.net.ClientConnectionEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickPresetKeybind(CallbackInfo ci) {
        CustomKeyMapping.armorHider$tickAll((Minecraft) (Object) this);
        LoadPresetKeyMapping.tick();
    }

    //? if < 1.20.5 {
    /*@Inject(method = "clearLevel()V", at = @At("HEAD"))
    *///?} elif < 1.21.8 {
    /*@Inject(method = "disconnect()V", at = @At("HEAD"))
    *///?} else {
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    //?}
    private void onDisconnect(CallbackInfo ci) {
        ClientConnectionEvents.onClientDisconnect((Minecraft) (Object) this);
    }
}
