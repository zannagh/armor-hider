package de.zannagh.armorhider.client.keybinds;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class LoadPresetKeyMapping extends CustomKeyMapping {

    // Left-Alt is uncommon among vanilla defaults. GLFW keycodes only — an AWT VK
    // value here would mis-map; VK_UNDEFINED (0) in particular collides with the
    // number-row "0" key (both named "key.keyboard.0").
    public static final int DEFAULT_KEY = GLFW.GLFW_KEY_LEFT_ALT;

    private static LoadPresetKeyMapping instance;
    private int activatedWhileHeld = -1;

    public LoadPresetKeyMapping(int preferredKey) {
        super("key.armorhider.preset", preferredKey);
        instance = this;
    }

    @Override
    public void onKeyDown() {}

    @Override
    public void onKeyUp() {
        activatedWhileHeld = -1;
    }

    public static void tick() {
        if (instance == null || !instance.isDown()) {
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
