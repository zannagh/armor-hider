//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
//? if > 1.21.4
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Drives an FCGT client onto an <b>external</b> (already running) dedicated server and back off it
 * again.
 * <p>
 * FCGT's own {@code worldBuilder()} only covers integrated singleplayer, and its
 * {@code TestDedicatedServerContext} spawns a vanilla server from the dev classpath - neither can
 * reach a PaperMC process started outside Gradle. This helper therefore drives the vanilla
 * multiplayer connect path directly: build a throwaway {@link ServerData}, hand it to
 * {@link ConnectScreen#startConnecting}, then poll until the client has a level and a player.
 * <p>
 * Only two things differ across the FCGT-capable variants and both are stonecutter-gated here: the
 * current-screen accessor (moved to {@code Minecraft#gui().screen()} after 26.1.2) and the
 * disconnect pair (gained a reason {@link Component} and was renamed to
 * {@code disconnectWithSavingScreen} after 1.21.4). {@code ConnectScreen.startConnecting},
 * {@code ServerAddress.parseString} and the {@code ServerData} constructor are byte-identical on
 * every variant, so they are not gated.
 */
final class TestServerConnect {

    /** Ticks to wait for the TCP connect + login + join sequence to complete (~30 s at 20 TPS). */
    private static final int CONNECT_TIMEOUT_TICKS = 600;

    /** Ticks to wait for the client to tear the connection down again (~10 s at 20 TPS). */
    private static final int DISCONNECT_TIMEOUT_TICKS = 200;

    private TestServerConnect() {
    }

    /**
     * Connects the client to {@code host:port} and blocks until the player has joined a level.
     *
     * @param context the FCGT context driving this test
     * @param host    hostname or literal IP of the already-running external server
     * @param port    the server's port
     * @throws IllegalStateException if no world/player materialised before the timeout
     */
    static void connect(ClientGameTestContext context, String host, int port) {
        String ip = host + ":" + port;
        ArmorHider.LOGGER.info("[smoke/fcgt] Connecting to external server at {}", ip);

        context.waitForScreen(TitleScreen.class);
        context.runOnClient(client -> {
            // Presence of a parent screen only matters for the "back" button on connection failure,
            // but pass the real one so a failed connect lands somewhere sane instead of on null.
            //? if <= 26.1.2
            //var parent = client.screen;
            //? if > 26.1.2
            var parent = client.gui.screen();
            var address = ServerAddress.parseString(ip);
            var data = new ServerData("armorhider-smoke", ip, ServerData.Type.OTHER);
            // Trailing null = TransferState; there is exactly one overload on every FCGT variant,
            // so the bare null resolves without an import.
            ConnectScreen.startConnecting(parent, client, address, data, false, null);
        });

        try {
            context.waitFor(client -> client.level != null && client.player != null, CONNECT_TIMEOUT_TICKS);
            // FCGT signals a wait timeout with a bare AssertionError("Timed out waiting for predicate"),
            // NOT a RuntimeException - catching only the latter would let that useless message escape.
        } catch (AssertionError | RuntimeException e) {
            throw new IllegalStateException(
                    "[smoke/fcgt] Never joined the external server at " + ip + " within "
                            + CONNECT_TIMEOUT_TICKS + " ticks - is the Paper server actually running on that"
                            + " port, in offline mode, and is online-mode/whitelist off for the smoke player?", e);
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] Joined external server at {}", ip);
    }

    /**
     * Disconnects from the current server and returns to the title screen.
     * <p>
     * Mandatory before the test method returns: FCGT's runner asserts the final client state is
     * "no server running, {@code level == null}, current screen is {@link TitleScreen}".
     *
     * @param context the FCGT context driving this test
     */
    static void disconnect(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Disconnecting from external server");
        context.runOnClient(client -> {
            if (client.level == null) {
                return;
            }
            //? if <= 1.21.4 {
            /*client.level.disconnect();
            client.disconnect();
            *///?}
            //? if > 1.21.4 {
            client.level.disconnect(Component.literal("armor-hider smoke test finished"));
            client.disconnectWithSavingScreen();
            //?}
        });
        context.waitFor(client -> client.level == null, DISCONNECT_TIMEOUT_TICKS);
        // Hand back to the title via FCGT's own screen setter (Minecraft's instance method of that
        // name is gone on 26.2). The method REFERENCE below is deliberate and must not be inlined
        // into a direct call: a global stonecutter replacement rewrites that exact call text into
        // Minecraft's `setScreenAndShow` form on >= 1.21.9, which would mangle this FCGT call.
        // Going through a Consumer keeps the rewrite from matching.
        Consumer<Supplier<Screen>> screenSetter = context::setScreen;
        screenSetter.accept(TitleScreen::new);
        context.waitForScreen(TitleScreen.class);
    }
}
//?}
