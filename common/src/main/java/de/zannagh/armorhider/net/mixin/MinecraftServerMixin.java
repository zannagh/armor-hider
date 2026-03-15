package de.zannagh.armorhider.net.mixin;

import de.zannagh.armorhider.server.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;initServer()Z"))
    private void onServerStarting(CallbackInfo ci) {
        ServerLifecycleEvents.onServerStarting((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void onServerStopping(CallbackInfo ci) {
        ServerLifecycleEvents.onServerStopping((MinecraftServer) (Object) this);
    }
}
