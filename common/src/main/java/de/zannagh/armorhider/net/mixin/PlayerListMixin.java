package de.zannagh.armorhider.net.mixin;

import de.zannagh.armorhider.server.ServerConnectionEvents;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Raises the server-side join event at the moment the player actually enters the player list.
 *
 * <p><b>Why not the login listener.</b> This used to hang off
 * {@code ServerLoginPacketListenerImpl}, and on 1.20.1 that was correct - {@code handleAcceptedLogin}
 * calls {@code placeNewPlayer} itself, so at its tail the player really was in the list. From 1.20.5 on
 * it is not. {@code finishLoginAndWaitForClient} only sets {@code PROTOCOL_SWITCHING} and sends
 * {@code ClientboundLoginFinishedPacket}; the player reaches {@code playersByUUID} only after the whole
 * CONFIGURATION phase - registry sync, the resource-pack download, the client-information reply, the
 * code-of-conduct task on 26.x, {@code PrepareSpawnTask}'s radius-3 chunk load, and any mod-loader
 * handshakes queued alongside. That window is client-driven and unbounded; vanilla's own 30-second
 * login watchdog stops ticking at the handoff and configuration has no timeout at all.
 *
 * <p>The old hook bridged the gap by polling the player list on a {@code ForkJoinPool.commonPool} thread
 * behind an exponential backoff that expired after ~4.3 s. On a memory connection that poll wins in
 * milliseconds, which is why it was green everywhere we test; on a real server with a large modpack it
 * loses, and losing it silently skipped every join handler - no server config, no permission level, no
 * shared-rule sync. To the player that is indistinguishable from a server without the mod. See #375.</p>
 *
 * <p><b>Injected at TAIL</b>, the first point where the player is in {@code players}, in
 * {@code playersByUUID}, added to its level and holding an inventory menu, and where the client has
 * already received {@code ClientboundLoginPacket} and the initial teleport - so packets a handler sends
 * will land. {@code ServerGamePacketListenerImpl}'s construction, some fifty lines earlier in the same
 * method, is not a substitute: the list insert has not happened there yet.</p>
 *
 * <p>The target is split by arity only because 1.20.1 predates {@code CommonListenerCookie}. Both
 * descriptors were verified with {@code javap} against the mapped {@code minecraft-common} jar for every
 * supported version: exactly one {@code placeNewPlayer} overload on each, and the 3-arg descriptor is
 * byte-identical from 1.21.1 through 26.3.</p>
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    //? if >= 1.20.2 {
    @Inject(
            method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At("TAIL"))
    private void ah$onPlayerJoin(
            Connection connection,
            ServerPlayer player,
            net.minecraft.server.network.CommonListenerCookie cookie,
            CallbackInfo callbackInfo) {
        ServerConnectionEvents.onPlayerJoin(player, server);
    }
    //?}

    //? if < 1.20.2 {
    /*@Inject(
            method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("TAIL"))
    private void ah$onPlayerJoin(
            Connection connection,
            ServerPlayer player,
            CallbackInfo callbackInfo) {
        ServerConnectionEvents.onPlayerJoin(player, server);
    }
    *///?}
}
