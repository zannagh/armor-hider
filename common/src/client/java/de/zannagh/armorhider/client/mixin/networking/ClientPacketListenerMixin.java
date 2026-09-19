//? if >= 1.20.5 {
package de.zannagh.armorhider.client.mixin.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.net.ClientConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mixin to bridge the client player-join event. S2C payload dispatch is handled by eunomia's transport.
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {

    protected ClientPacketListenerMixin(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onHandleLogin(CallbackInfo ci) {
        var listener = (ClientPacketListener) (Object) this;
        ArmorHider.LOGGER.info("Client joined server (client-side)");
        ClientConnectionEvents.onClientJoin(listener, minecraft);
    }
}
//?}
