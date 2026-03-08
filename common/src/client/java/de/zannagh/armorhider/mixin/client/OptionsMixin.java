package de.zannagh.armorhider.mixin.client;

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
        boolean wereOptionsAddedAlready = Arrays.stream(keyMappings).anyMatch(map -> map.getName().equals(ToggleOffKeyMapping.MAPPING_NAME));
        if (wereOptionsAddedAlready) {
            return;
        }
        var armorHiderKeyMapping = new ToggleOffKeyMapping();
        var openSettingsKeyMapping = new OpenSettingsKeyMapping();
        var existingMappings = new java.util.ArrayList<>(Arrays.stream(keyMappings.clone()).toList());
        existingMappings.add(armorHiderKeyMapping);
        existingMappings.add(openSettingsKeyMapping);
        keyMappings = existingMappings.toArray(new KeyMapping[0]);
    }
}
