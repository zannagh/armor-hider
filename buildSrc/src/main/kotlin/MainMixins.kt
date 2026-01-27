import dev.kikugie.stonecutter.data.ParsedVersion

class MainMixins
{
    val parsedVersion: ParsedVersion
    
    constructor(parsedVersion: ParsedVersion) {
        this.parsedVersion = parsedVersion
    }

    override fun toString(): String {
            var returnString =
                "networking.MinecraftServerMixin\",\n" +
                        "    \"networking.ServerLoginMixin\",\n"
            if (parsedVersion >= "1.20.5") {
                returnString +=
                    "    \"networking.CustomPayloadCodecMixin\",\n" +
                            "    \"networking.ServerGamePacketListenerMixin"
            } else {
                returnString +=
                    "    \"networking.ServerPlayNetworkHandlerMixin"
            }
            return returnString;
        
    }
}