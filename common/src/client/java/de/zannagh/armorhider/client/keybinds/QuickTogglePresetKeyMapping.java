package de.zannagh.armorhider.client.keybinds;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.utils.McClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.awt.event.KeyEvent;

/**
 * Alternates the local player's own appearance between the two presets chosen in the advanced settings.
 * Touches neither the session toggle nor any social/server setting.
 */
public class QuickTogglePresetKeyMapping extends CustomKeyMapping {

    public static final int DEFAULT_KEY = KeyEvent.VK_P;

    private static volatile boolean keyTakenNoticePending;

    public QuickTogglePresetKeyMapping() {
        super("key.armorhider.preset_quick_toggle", DEFAULT_KEY);
    }

    /** Called when the default key was already used elsewhere and this mapping was left unbound. */
    public static void markKeyTaken() {
        keyTakenNoticePending = true;
    }

    /** Shows the "key already taken" toast once, as soon as a world is joined. Called every client tick. */
    public static void tickNotice(Minecraft minecraft) {
        if (!keyTakenNoticePending || minecraft == null || minecraft.player == null) {
            return;
        }
        keyTakenNoticePending = false;
        McClientUtils.showToast(
                Component.translatable("armorhider.toast.quick_toggle_key_taken.title"),
                Component.translatable("armorhider.toast.quick_toggle_key_taken"));
    }

    @Override
    protected void armorHider$onActivated() {
        var presetManager = ArmorHiderClient.PRESET_MANAGER;
        LoadPresetKeyMapping.activatePreset(presetManager.resolveQuickToggleTarget());
    }
}
