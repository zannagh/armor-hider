package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.keybinds.CustomKeyMapping;
import de.zannagh.armorhider.client.keybinds.LoadPresetKeyMapping;
import de.zannagh.armorhider.client.net.ClientConnectionEvents;
import de.zannagh.armorhider.client.net.SharedRuleBroadcaster;
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
        // Shared render rules are announced from the tick, not the render path: the local player's
        // state has to exist whether or not anything is currently drawing them. Costs one volatile
        // read while no mod has registered a shared rule.
        SharedRuleBroadcaster.tick((Minecraft) (Object) this);
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
