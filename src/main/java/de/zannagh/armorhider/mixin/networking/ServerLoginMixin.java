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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginMixin implements ServerLoginPacketListener, TickablePacketListener {

    @Final
    @Shadow
    MinecraftServer server;
    
    //? if >= 1.21 {
    
    @Shadow
    private GameProfile authenticatedProfile;
    //?}
    
    //? if < 1.21 {
    /*@Shadow
    GameProfile gameProfile;
    *///?}

    //? if >= 1.20.5 {
    @Inject(method = "finishLoginAndWaitForClient", at = @At(value = "TAIL"))
    private void handlePlayerJoin(CallbackInfo ci) {
        raiseLoginEvent();
    }
    //?}
    //? if < 1.20.5 {
    /*@Inject(method = "handleAcceptedLogin", at = @At(value = "TAIL"))
    private void handlePlayerJoin(CallbackInfo ci) {
        raiseLoginEvent();
    }
    *///?}
    
    @Unique
    private void raiseLoginEvent(){
        //? if < 1.21
        //var authenticatedProfile = gameProfile;
        if (authenticatedProfile == null) {
            return;
        }
        ServerConnectionEvents.onPlayerJoin(authenticatedProfile, server);
    }
}
