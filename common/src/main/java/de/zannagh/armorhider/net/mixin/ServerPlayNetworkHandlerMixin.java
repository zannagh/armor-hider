//? if < 1.20.5 {
/*package de.zannagh.armorhider.net.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

// Legacy (< 1.20.5) serverbound custom-payload handling was hand-rolled here. With the eunomia
// networking migration the transport (codec injection + payload dispatch) is supplied by the eunomia
// mod on every game version, so this mixin no longer carries any injections - it is kept only so the
// mixin config entry still resolves on the pre-1.20.5 variants.
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin {
}
*///?}
