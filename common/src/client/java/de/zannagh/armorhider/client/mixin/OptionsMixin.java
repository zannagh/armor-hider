package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.keybinds.OpenSettingsKeyMapping;
import de.zannagh.armorhider.keybinds.ToggleOffKeyMapping;
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
        if (hasToggleMapping && hasOpenSettingsMapping) {
            return;
        }
        var armorHiderKeyMapping = hasToggleMapping ? null : new ToggleOffKeyMapping();
        var openSettingsKeyMapping = hasOpenSettingsMapping ? null : new OpenSettingsKeyMapping();
        var existingMappings = new java.util.ArrayList<>(Arrays.stream(keyMappings.clone()).toList());
        if (armorHiderKeyMapping != null) {
            existingMappings.add(armorHiderKeyMapping);
        }
        if (openSettingsKeyMapping != null) {
            existingMappings.add(openSettingsKeyMapping);
        }
        keyMappings = existingMappings.toArray(new KeyMapping[0]);
    }
}
