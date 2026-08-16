//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.screens.ArmorHiderOptionsScreen;
import de.zannagh.armorhider.client.keybinds.CustomKeyMapping;
import de.zannagh.armorhider.client.keybinds.OpenSettingsKeyMapping;
import de.zannagh.armorhider.client.keybinds.ToggleOffKeyMapping;
import de.zannagh.armorhider.configuration.SettingsLocation;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Regression smoke for the mod's key mappings (settings-screen re-open loop).
 * <p>
 * The original bug: {@link CustomKeyMapping} ran its action from a {@code setDown} override. Vanilla calls
 * {@code setDown} for things that are not key presses - {@code KeyMapping.releaseAll()} on every screen open,
 * and {@code KeyMapping.setAll()} from {@code MouseHandler#grabMouse()} when a screen closes. The latter is
 * gated on {@code InputQuirks.RESTORE_KEY_STATE_AFTER_MOUSE_GRAB}, which is {@code !ON_OSX}, so on
 * Windows/Linux closing the settings screen while the key was still held re-opened it forever.
 * <p>
 * These checks pin the invariant that makes that impossible on <em>every</em> OS: {@code setDown} is inert,
 * and only a real press activates a mapping. A macOS host cannot exercise the {@code setAll()} path at all,
 * so "{@code setDown} does nothing" is the portable form of the same guarantee - on a Windows/Linux runner
 * {@link #assertClosingDoesNotReopen} additionally reproduces the original user action end to end.
 */
public final class KeybindSmokeTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Keybind smoke starting");
        context.waitForScreen(TitleScreen.class);

        try (var singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {

            // The open-settings action routes by settingsScreenLocation: SKIN_CUSTOMIZATION would open the
            // vanilla skin screen instead. Pin it to OPTIONS_SCREEN (which opens the standalone
            // ArmorHiderOptionsScreen) so the assertions below have one expected screen type.
            var priorSettingsLocation = context.computeOnClient(client ->
                    ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig()
                            .settingsScreenLocation.getValue());
            context.runOnClient(client -> ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig()
                    .settingsScreenLocation.setValue(SettingsLocation.OPTIONS_SCREEN));

            // Under -Pcompat=all a foreign mod frequently binds our defaults (J/K are common); vanilla's
            // KeyMapping.MAP is single-winner per physical key, so whichever mapping registered last owns
            // the slot and a physical press of that key increments only THAT mapping's click count. When a
            // compat mod wins, context.getInput().pressKey(ourMapping) presses the shared key but the click
            // routes to the foreign mapping and our activation never fires - a false red that reflects a
            // user-resolvable keybind conflict, not a mod bug. Rebind our mappings onto guaranteed-unused
            // function keys (F23/F24 - nothing in the vanilla+compat stack claims those) so each press
            // deterministically drives OUR mapping, exercising the real press -> tick -> activation path.
            reboundToFreeKeys(context);

            try {
                assertSetDownIsInert(context);
                assertPressOpensSettings(context);
                assertClosingDoesNotReopen(context);
                assertToggleKeyFlipsSessionOverride(context);
            } finally {
                restoreDefaultKeys(context);
                context.runOnClient(client -> ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig()
                        .settingsScreenLocation.setValue(priorSettingsLocation));
            }

            ArmorHider.LOGGER.info("[smoke/fcgt] Keybind checks passed");
        }
    }

    /**
     * The regression guard. Driving {@code setDown} directly - exactly what {@code KeyMapping.setAll()} does
     * on a mouse re-grab - must not open anything. This is the OS-independent form of the fix.
     */
    private static void assertSetDownIsInert(ClientGameTestContext context) {
        context.runOnClient(client -> {
            closeAnyScreen(client);
            var openSettings = mapping(client, OpenSettingsKeyMapping.class);
            openSettings.setDown(true);
            openSettings.setDown(false);
        });
        context.waitTicks(5);
        context.runOnClient(client -> {
            Screen screen = currentScreen(client);
            if (screen != null) {
                throw new IllegalStateException("[smoke/fcgt] setDown opened a screen ("
                        + screen.getClass().getName() + ") - the re-open loop is back");
            }
        });
        ArmorHider.LOGGER.info("[smoke/fcgt] setDown is inert");
    }

    /** A real key press must open the settings screen. */
    private static void assertPressOpensSettings(ClientGameTestContext context) {
        context.runOnClient(KeybindSmokeTest::closeAnyScreen);
        // Hoisted to a typed local: passing computeOnClient(...) straight into pressKey leaves T open and
        // makes the KeyMapping / Function<Options, KeyMapping> overloads ambiguous.
        final KeyMapping openSettings = resolveMapping(context, OpenSettingsKeyMapping.class);
        context.getInput().pressKey(openSettings);
        context.waitTicks(5);
        context.runOnClient(client -> {
            Screen screen = currentScreen(client);
            if (!(screen instanceof ArmorHiderOptionsScreen)) {
                throw new IllegalStateException(
                        "[smoke/fcgt] the open-settings keybind did not open ArmorHiderOptionsScreen (got "
                                + (screen == null ? "null" : screen.getClass().getName()) + ")");
            }
        });
        ArmorHider.LOGGER.info("[smoke/fcgt] key press opens the settings screen");
    }

    /**
     * The original user action: close the settings screen while the bound key is still physically held. On
     * Windows/Linux the old code re-opened it here via {@code grabMouse() -> setAll()}.
     */
    private static void assertClosingDoesNotReopen(ClientGameTestContext context) {
        final KeyMapping openSettings = resolveMapping(context, OpenSettingsKeyMapping.class);

        context.getInput().holdKey(openSettings);
        context.runOnClient(client -> {
            Screen screen = currentScreen(client);
            if (screen == null) {
                throw new IllegalStateException("[smoke/fcgt] expected the settings screen to still be open");
            }
            screen.onClose();
        });
        context.waitTicks(10);
        try {
            context.runOnClient(client -> {
                Screen screen = currentScreen(client);
                if (screen != null) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] a screen re-opened after closing the settings screen with the key held ("
                                    + screen.getClass().getName() + ")");
                }
            });
        } finally {
            context.getInput().releaseKey(openSettings);
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] closing with the key held does not re-open");
    }

    /** The toggle keybind still works through the press path. */
    private static void assertToggleKeyFlipsSessionOverride(ClientGameTestContext context) {
        boolean before = context.computeOnClient(client ->
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.hasSessionDisableOverride());

        final KeyMapping toggle = resolveMapping(context, ToggleOffKeyMapping.class);
        context.getInput().pressKey(toggle);
        context.waitTicks(5);

        boolean after = context.computeOnClient(client ->
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.hasSessionDisableOverride());
        if (after == before) {
            throw new IllegalStateException(
                    "[smoke/fcgt] the toggle keybind did not flip the session disable override");
        }

        context.runOnClient(client -> ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride());
        ArmorHider.LOGGER.info("[smoke/fcgt] toggle keybind flips the session override");
    }

    /**
     * The mod's mappings, each paired with a guaranteed-unused physical key. F23/F24 are registered
     * vanilla key names that nothing in the vanilla controls or the compat stack binds, so rebinding
     * onto them makes {@code getInput().pressKey(mapping)} route to OUR mapping deterministically.
     */
    private static final Map<Class<? extends CustomKeyMapping>, String> FREE_KEYS = new LinkedHashMap<>();

    static {
        FREE_KEYS.put(OpenSettingsKeyMapping.class, "key.keyboard.f24");
        FREE_KEYS.put(ToggleOffKeyMapping.class, "key.keyboard.f23");
    }

    /**
     * Rebinds each of the mod's key mappings onto its distinct free key (see {@link #FREE_KEYS}) so a
     * simulated press cannot be stolen by a compat mod that claimed our default (on &lt;=1.21.8 vanilla's
     * {@code KeyMapping.MAP} is single-winner per key, so a colliding foreign binding would swallow the
     * click entirely). {@link #restoreDefaultKeys} puts them back afterwards.
     */
    private static void reboundToFreeKeys(ClientGameTestContext context) {
        context.runOnClient(client -> {
            FREE_KEYS.forEach((type, keyName) ->
                    mapping(client, type).setKey(InputConstants.getKey(keyName)));
            KeyMapping.resetMapping();
        });
    }

    /**
     * Restores each rebound mapping to its default key. FCGT runs with consistent/default settings, so
     * the default is exactly what the key was before {@link #reboundToFreeKeys}; this keeps sibling
     * scenarios in the same batched launch unaffected.
     */
    private static void restoreDefaultKeys(ClientGameTestContext context) {
        context.runOnClient(client -> {
            FREE_KEYS.keySet().forEach(type -> {
                KeyMapping mapping = mapping(client, type);
                mapping.setKey(mapping.getDefaultKey());
            });
            KeyMapping.resetMapping();
        });
    }

    private static KeyMapping resolveMapping(ClientGameTestContext context,
                                             Class<? extends CustomKeyMapping> type) {
        return context.computeOnClient((Minecraft client) -> (KeyMapping) mapping(client, type));
    }

    private static <T extends CustomKeyMapping> T mapping(Minecraft client, Class<T> type) {
        for (KeyMapping candidate : client.options.keyMappings) {
            if (type.isInstance(candidate)) {
                return type.cast(candidate);
            }
        }
        throw new IllegalStateException("[smoke/fcgt] " + type.getSimpleName() + " is not registered");
    }

    private static @Nullable Screen currentScreen(Minecraft client) {
        //? if <= 26.1.2
        //return client.screen;
        //? if > 26.1.2
        return client.gui.screen();
    }

    private static void closeAnyScreen(Minecraft client) {
        Screen screen = currentScreen(client);
        if (screen != null) {
            screen.onClose();
        }
        if (currentScreen(client) != null) {
            throw new IllegalStateException("[smoke/fcgt] could not reach a screenless state");
        }
    }
}
//?}
