package de.zannagh.armorhider.client.keybinds;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Base class for the mod's key mappings.
 * <p>
 * Actions are edge-triggered from the client tick via {@link #consumeClick()}, never from a
 * {@code setDown} override. Vanilla calls {@code setDown} for reasons that are not key presses:
 * {@code Minecraft#setScreen(non-null)} runs {@code KeyMapping.releaseAll()}, and closing a screen
 * runs {@code MouseHandler#grabMouse()} which calls {@code KeyMapping.setAll()} whenever
 * {@code InputQuirks.RESTORE_KEY_STATE_AFTER_MOUSE_GRAB} is set — that flag is {@code !ON_OSX}, so it
 * is on for Windows/Linux and off for macOS. Running an action from {@code setDown} therefore made
 * "close a screen while the bound key is physically held" re-fire the action, which turned the
 * open-settings binding into an inescapable re-open loop on Windows/Linux only.
 * <p>
 * Click counts are incremented solely for genuine presses and are zeroed by {@code release()} on
 * every screen open, so this path cannot re-fire on a screen transition and behaves identically on
 * every OS. The trade-off is that presses are ignored while a screen is open, matching how vanilla
 * and every other mod's keybinds behave.
 */
public abstract class CustomKeyMapping extends KeyMapping {

    public CustomKeyMapping(String name, int preferredKey) {
        //? if > 1.21.8
        super(name, preferredKey, Category.MISC);
        //? if <= 1.21.8
         //super(name, preferredKey, "key.categories.misc");
    }

    /**
     * Drains pending presses for every armor-hider mapping. Called once per client tick.
     * Iterates the live options array rather than a static registry so mappings can never leak.
     */
    public static void armorHider$tickAll(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (mapping instanceof CustomKeyMapping custom) {
                custom.armorHider$drainClicks();
            }
        }
    }

    /**
     * Collapses however many presses queued up since the last tick into a single activation, so a
     * burst of presses can never open one screen per press.
     */
    private void armorHider$drainClicks() {
        boolean pressed = false;
        while (consumeClick()) {
            pressed = true;
        }
        if (pressed) {
            armorHider$onActivated();
        }
    }

    /** Runs once per press, on the client tick following it. */
    protected abstract void armorHider$onActivated();
}
