package de.zannagh.armorhider.mixin.networking;

import com.mojang.authlib.GameProfile;
import de.zannagh.armorhider.net.ServerConnectionEvents;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginMixin implements ServerLoginPacketListener, TickablePacketListener {

    @Shadow
    private GameProfile authenticatedProfile;
    
    @Final
    @Shadow
    MinecraftServer server;
    
    @Inject(method = "finishLoginAndWaitForClient", at = @At(value = "TAIL"))
    private void handlePlayerJoin(CallbackInfo ci) {
        var player = this.server.getPlayerList().getPlayer(authenticatedProfile.id());
        if (player != null) {
            ServerConnectionEvents.onPlayerJoin(player, server);
        }
    }
}
