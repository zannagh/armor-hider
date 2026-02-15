import dev.kikugie.stonecutter.data.ParsedVersion

class MainMixins
{
    val parsedVersion: ParsedVersion
    
    constructor(parsedVersion: ParsedVersion) {
        this.parsedVersion = parsedVersion
    }

    override fun toString(): String {
        val mixinStringBuilder = MixinStringBuilder("networking.MinecraftServerMixin")
        mixinStringBuilder.addMixin("networking.ServerLoginMixin")
        if (parsedVersion >= "1.20.5") {
            mixinStringBuilder.addMixin("networking.CustomPayloadCodecMixin")
            return mixinStringBuilder.getMixinString("networking.ServerGamePacketListenerMixin")
        } else {
            return mixinStringBuilder.getMixinString("networking.ServerPlayNetworkHandlerMixin")
        }
    }
}