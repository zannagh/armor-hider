import dev.kikugie.stonecutter.data.ParsedVersion

class MainMixins(
    val parsedVersion: ParsedVersion,
    val loader: String = "fabric"
) {
    override fun toString(): String {
        val mixinStringBuilder = MixinStringBuilder("networking.MinecraftServerMixin")
        mixinStringBuilder.addMixin("networking.ServerLoginMixin")
        if (parsedVersion >= "1.20.5") {
            if (loader != "neoforge") {
                // NeoForge: CustomPayloadCodecMixin uses @ModifyVariable on an interface (unsupported)
                mixinStringBuilder.addMixin("networking.CustomPayloadCodecMixin")
                // NeoForge: payload dispatch handled natively via RegisterPayloadHandlersEvent
                mixinStringBuilder.addMixin("networking.ServerGamePacketListenerMixin")
            }
            return mixinStringBuilder.getMixinString()
        } else {
            return mixinStringBuilder.getMixinString("networking.ServerPlayNetworkHandlerMixin")
        }
    }
}