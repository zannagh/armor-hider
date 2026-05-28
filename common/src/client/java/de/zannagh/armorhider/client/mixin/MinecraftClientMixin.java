package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.keybinds.LoadPresetKeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickPresetKeybind(CallbackInfo ci) {
        LoadPresetKeyMapping.tick();
    }
}
