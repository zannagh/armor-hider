import dev.kikugie.stonecutter.data.ParsedVersion

class ClientMixins {

    val parsedVersion: ParsedVersion
    
    constructor(parsedVersion: ParsedVersion) {
        this.parsedVersion = parsedVersion
    }
    
    fun getScreenMixinString(): String {
        // For 1.20.x: Use OptionsScreenMixin (injects into main options screen)
        // For 1.21+: Use SkinOptionsMixin (injects into skin options screen)
        return if (parsedVersion > "1.21.1") {
            "SkinOptionsMixin"
        } else {
            "OptionsScreenMixin"
        }
    }
    
    override fun toString(): String {
        var returnString = ""
        
        if (parsedVersion > "1.21.1") {
            returnString += "bodyKneesAndToes.EquipmentRenderMixin\",\n"
            returnString += "    \"bodyKneesAndToes.ArmorFeatureRenderMixin\",\n"
        } else {
            returnString += "bodyKneesAndToes.HumanoidArmorLayerMixin\",\n"
        }
        
        if (parsedVersion >= "1.21.11") {
            returnString += "    \"hand.ItemInHandLayerMixin\",\n"
            returnString += "    \"hand.OffHandRenderMixin\",\n"
            returnString += "    \"hand.ItemRenderMixin\",\n"
            returnString += "    \"hand.ItemRenderStateMixin\",\n"
            returnString += "    \"hand.ModelPartSubmitMixin\",\n"
        }

        if (parsedVersion >= "1.20.5") {
            returnString += "    \"networking.ClientPacketListenerMixin"
        } else {
            returnString += "    \"networking.ClientPlayNetworkHandlerMixin"
        }
        return returnString;
    }
}