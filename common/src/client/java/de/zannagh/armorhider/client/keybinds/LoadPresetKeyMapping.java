package de.zannagh.armorhider.client.keybinds;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.client.Minecraft;
//? if >= 26.3-0.snapshot.5 {
/*import com.mojang.blaze3d.platform.InputConstants;
*///?} else {
import org.lwjgl.glfw.GLFW;
//?}

public class LoadPresetKeyMapping extends CustomKeyMapping {

    // Left-Alt is uncommon among vanilla defaults. GLFW keycodes only — an AWT VK
    // value here would mis-map; VK_UNDEFINED (0) in particular collides with the
    // number-row "0" key (both named "key.keyboard.0").
    //? if >= 26.3-0.snapshot.5 {
    /*public static final int DEFAULT_KEY = InputConstants.KEY_LALT;
    *///?} else {
    public static final int DEFAULT_KEY = GLFW.GLFW_KEY_LEFT_ALT;
    //?}

    private static LoadPresetKeyMapping instance;
    private int activatedWhileHeld = -1;

    public LoadPresetKeyMapping(int preferredKey) {
        super("key.armorhider.preset", preferredKey);
        instance = this;
    }

    // This mapping is a hold-modifier, not a press-action: the work happens in tick() while the key
    // is held down, so a press on its own does nothing.
    @Override
    protected void armorHider$onActivated() {}

    public static void tick() {
        if (instance == null) {
            return;
        }
        // isDown() is the right state to read for a hold-modifier, and it is safe to read: vanilla
        // clears it via releaseAll() when a screen opens and restores it via setAll() when one
        // closes. Resetting here (rather than from a setDown override) keeps the latch honest on
        // both paths and on every OS.
        if (!instance.isDown()) {
            instance.activatedWhileHeld = -1;
            return;
        }
        var mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return;
        }

        for (int i = 0; i < 5 && i < mc.options.keyHotbarSlots.length; i++) {
            var hotbarKey = mc.options.keyHotbarSlots[i];
            if (hotbarKey.isDown() && instance.activatedWhileHeld != i) {
                instance.activatedWhileHeld = i;
                instance.activatePreset(i);
                while (hotbarKey.consumeClick()) {}
                return;
            }
        }
    }

    private void activatePreset(int presetIndex) {
        var presetManager = ArmorHiderClient.PRESET_MANAGER;
        if (!presetManager.hasPreset(presetIndex)) {
            return;
        }
        var preset = presetManager.getPreset(presetIndex);
        if (preset == null) {
            return;
        }
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig();
        presetManager.setActiveIndex(presetIndex);
        preset.applyTo(config);
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
        ArmorHider.LOGGER.info("Loaded preset {} via keybind.", presetIndex + 1);
    }
}
