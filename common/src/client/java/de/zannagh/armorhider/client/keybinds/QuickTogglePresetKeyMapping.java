package de.zannagh.armorhider.client.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.utils.McClientUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.awt.event.KeyEvent;

/**
 * Alternates the local player's own appearance between the two presets chosen in the advanced settings.
 * Touches neither the session toggle nor any social/server setting.
 */
public class QuickTogglePresetKeyMapping extends CustomKeyMapping {

    public static final int DEFAULT_KEY = KeyEvent.VK_O;

    public QuickTogglePresetKeyMapping() {
        super("key.armorhider.preset_quick_toggle", DEFAULT_KEY);
    }

    /**
     * Runs every client tick, cheaply. Once per install (persisted) it checks whether the default key is
     * already used by another mapping and leaves this one unbound if so, owing the player a toast. The
     * owed toast is persisted too and shown on the first world join, so quitting before joining loses nothing.
     * Done from the tick rather than from options loading so the preset file is guaranteed to be available.
     */
    public static void tickNotice(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        var presets = ArmorHiderClient.PRESET_MANAGER;
        if (!presets.isQuickToggleKeyChecked()) {
            resolveDefaultKeyConflict(minecraft);
        }
        if (presets.isQuickToggleNoticePending() && minecraft.player != null) {
            presets.setQuickToggleKeyState(true, false);
            McClientUtils.showToast(
                    Component.translatable("armorhider.toast.quick_toggle_key_taken.title"),
                    Component.translatable("armorhider.toast.quick_toggle_key_taken"));
        }
    }

    private static void resolveDefaultKeyConflict(Minecraft minecraft) {
        QuickTogglePresetKeyMapping own = null;
        for (KeyMapping map : minecraft.options.keyMappings) {
            if (map instanceof QuickTogglePresetKeyMapping q) {
                own = q;
            }
        }
        if (own == null) {
            return; // API-only mode: no mapping, nothing to check (and nothing to mark as checked).
        }
        String current = own.saveString();
        boolean untouchedDefault = current.equals(own.getDefaultKey().getName());
        boolean taken = false;
        for (KeyMapping map : minecraft.options.keyMappings) {
            taken |= map != own && map.saveString().equals(current);
        }
        if (untouchedDefault && taken) {
            own.setKey(InputConstants.UNKNOWN);
            KeyMapping.resetMapping();
            minecraft.options.save();
        }
        ArmorHiderClient.PRESET_MANAGER.setQuickToggleKeyState(true, untouchedDefault && taken);
    }

    @Override
    protected void armorHider$onActivated() {
        int target = ArmorHiderClient.PRESET_MANAGER.resolveQuickToggleTarget();
        if (!LoadPresetKeyMapping.activatePreset(target)) {
            McClientUtils.showActionBar(Component.translatable("armorhider.toggle.preset_empty", target + 1));
        }
    }
}
