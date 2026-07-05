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

    // Legacy corrupt binding: older builds defaulted the preset key to keycode 0,
    // which serialized as "key.keyboard.0" and clobbered the number-row "0" key.
    @Unique
    private static final String ARMORHIDER_LEGACY_PRESET_KEY = "key.keyboard.0";

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
            existingMappings.add(new LoadPresetKeyMapping(pickPresetDefaultKey()));
        }
        keyMappings = existingMappings.toArray(new KeyMapping[0]);
    }

    private int pickPresetDefaultKey() {
        boolean defaultTaken = Arrays.stream(keyMappings)
                .anyMatch(map -> map.getDefaultKey().getValue() == LoadPresetKeyMapping.DEFAULT_KEY);
        return defaultTaken ? LoadPresetKeyMapping.UNBOUND_KEY : LoadPresetKeyMapping.DEFAULT_KEY;
    }

    // Reset a preset binding left on the legacy corrupt "0" value back to its default.
    // Runs after vanilla applies saved keys, so saveString reflects the persisted value.
    @Inject(
            method = "load",
            at = @At("RETURN")
    )
    private void onLoadReturn(CallbackInfo ci) {
        boolean migrated = false;
        for (KeyMapping map : keyMappings) {
            if (map instanceof LoadPresetKeyMapping && ARMORHIDER_LEGACY_PRESET_KEY.equals(map.saveString())) {
                map.setKey(map.getDefaultKey());
                migrated = true;
            }
        }
        if (migrated) {
            KeyMapping.resetMapping();
            ((Options) (Object) this).save();
        }
    }
}
