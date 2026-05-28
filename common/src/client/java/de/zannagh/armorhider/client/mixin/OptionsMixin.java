package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.keybinds.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

@Mixin(Options.class)
public class OptionsMixin {
    @Mutable
    @Shadow
    @Final
    public KeyMapping[] keyMappings;
    
    @Inject(
            method = "load",
            at = @At("HEAD")
    )
    private void onLoad(CallbackInfo ci){
        boolean hasToggleMapping = Arrays.stream(keyMappings).anyMatch(map -> map instanceof ToggleOffKeyMapping);
        boolean hasOpenSettingsMapping = Arrays.stream(keyMappings).anyMatch(map -> map instanceof OpenSettingsKeyMapping);
        boolean hasPresetMapping = Arrays.stream(keyMappings).anyMatch(map -> map instanceof LoadPresetKeyMapping);
        if (hasToggleMapping && hasOpenSettingsMapping && hasPresetMapping) {
            return;
        }
        var existingMappings = new java.util.ArrayList<>(Arrays.stream(keyMappings.clone()).toList());
        if (!hasToggleMapping) {
            existingMappings.add(new ToggleOffKeyMapping());
        }
        if (!hasOpenSettingsMapping) {
            existingMappings.add(new OpenSettingsKeyMapping());
        }
        if (!hasPresetMapping) {
            existingMappings.add(new LoadPresetKeyMapping());
        }
        keyMappings = existingMappings.toArray(new KeyMapping[0]);
    }
}
