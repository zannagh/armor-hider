//? if < 1.20.5 {
/*package de.zannagh.armorhider.client.mixin.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.net.ClientConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Legacy (< 1.20.5) client mixin. The clientbound custom-payload dispatch that used to live here moved
// to the eunomia mod's transport; only the client player-join bridge remains.
@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onHandleLogin(CallbackInfo ci) {
        var listener = (ClientPacketListener) (Object) this;
        ArmorHider.LOGGER.info("Client joined server (client-side)");
        ClientConnectionEvents.onClientJoin(listener, minecraft);
    }
}
*///?}
