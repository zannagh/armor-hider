package de.zannagh.armorhider.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.common.ArmorHiderOptionsAccess;
import de.zannagh.armorhider.client.keybinds.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

@Mixin(Options.class)
public class OptionsMixin implements ArmorHiderOptionsAccess {

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
        // API-only mode owns the whole keybind story: a consuming mod does not want Armor Hider's
        // mappings in its users' Controls screen. Checked here rather than in the tick, so in the
        // normal case (the flag set from a mod initializer, which runs before Options is constructed)
        // the mappings are never created at all and nothing has to be undone.
        if (ArmorHider.isApiOnly()) {
            return;
        }
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
            existingMappings.add(new LoadPresetKeyMapping(LoadPresetKeyMapping.DEFAULT_KEY));
        }
        keyMappings = existingMappings.toArray(new KeyMapping[0]);
    }

    // Resolve the preset modifier only when its *current* (post-load) binding is
    // problematic: the legacy corrupt "0" or our own Left-Alt default colliding with
    // another mapping. A deliberate, non-default user binding is never touched.
    @Inject(
            method = "load",
            at = @At("RETURN")
    )
    private void onLoadReturn(CallbackInfo ci) {
        LoadPresetKeyMapping preset = null;
        for (KeyMapping map : keyMappings) {
            if (map instanceof LoadPresetKeyMapping p) {
                preset = p;
                break;
            }
        }
        if (preset == null) {
            return;
        }

        String currentKey = preset.saveString();
        String defaultKey = preset.getDefaultKey().getName();
        boolean isResolvable = ARMORHIDER_LEGACY_PRESET_KEY.equals(currentKey) || defaultKey.equals(currentKey);
        if (!isResolvable || !isKeyInUse(preset, currentKey)) {
            return;
        }

        preset.setKey(isKeyInUse(preset, defaultKey) ? InputConstants.UNKNOWN : preset.getDefaultKey());
        KeyMapping.resetMapping();
        ((Options) (Object) this).save();
    }

    /**
     * Undo path for {@link ArmorHider#useAsApiOnly()} being called after {@code load} already ran -
     * a consuming mod that flips the switch from its own (late) config, say. Dropping the mappings
     * from this array takes them out of the Controls screen and out of {@code save()}; they stay in
     * {@code KeyMapping.ALL} because vanilla never offers a way out of it, but nothing drains their
     * clicks any more (see {@code CustomKeyMapping#armorHider$tickAll}), so they are inert.
     */
    @Override
    public boolean armorHider$removeKeyMappings() {
        KeyMapping[] remaining = Arrays.stream(keyMappings)
                .filter(map -> !(map instanceof CustomKeyMapping))
                .toArray(KeyMapping[]::new);
        if (remaining.length == keyMappings.length) {
            return false;
        }
        keyMappings = remaining;
        KeyMapping.resetMapping();
        return true;
    }

    private boolean isKeyInUse(KeyMapping exclude, String keyName) {
        return Arrays.stream(keyMappings)
                .anyMatch(map -> map != exclude && map.saveString().equals(keyName));
    }
}
