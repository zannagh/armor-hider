package de.zannagh.armorhider.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.config.OpenSettingsKeyMapping;
import de.zannagh.armorhider.config.ToggleOffKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.event.KeyEvent;
import java.util.Arrays;

@Mixin(Options.class)
public class OptionsMixin {

    @Unique
    private static final String TOGGLE_KEY_NAME = "Toggle Armor Hider";
    
    @Mutable
    @Shadow
    @Final
    public KeyMapping[] keyMappings;
    
    @Inject(
            method = "load",
            at = @At("HEAD")
    )
    private void onLoad(CallbackInfo ci){
        // TODO: Localisation of name
        if (Arrays.stream(keyMappings).anyMatch(map -> map.getName().equals("Toggle Armor Hider"))) {
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
