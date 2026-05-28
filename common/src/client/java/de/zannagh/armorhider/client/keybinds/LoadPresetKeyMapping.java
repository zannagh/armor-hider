package de.zannagh.armorhider.client.keybinds;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.configuration.ConfigPreset;
import net.minecraft.client.Minecraft;

import java.awt.event.KeyEvent;

public class LoadPresetKeyMapping extends CustomKeyMapping {

    private static LoadPresetKeyMapping instance;
    private int activatedWhileHeld = -1;

    public LoadPresetKeyMapping() {
        super("key.armorhider.preset", KeyEvent.VK_UNDEFINED);
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
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue();
        presetManager.setActiveIndex(presetIndex);
        preset.applyTo(config);
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
        ArmorHider.LOGGER.info("Loaded preset {} via keybind.", presetIndex + 1);
    }
}
