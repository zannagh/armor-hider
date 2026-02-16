import dev.kikugie.stonecutter.data.ParsedVersion

class ClientMixins {

    val parsedVersion: ParsedVersion
    
    constructor(parsedVersion: ParsedVersion) {
        this.parsedVersion = parsedVersion
    }
    
    fun getScreenMixinString(): String {
        // For 1.20.x: Use OptionsScreenMixin (injects into main options screen)
        // For 1.21+: Use SkinOptionsMixin (injects into skin options screen)
        return if (parsedVersion > "1.21.6") {
            "SkinOptionsMixin"
        } else {
            "OptionsScreenMixin"
        }
    }
    
    override fun toString(): String {
        val mixinStringBuilder = MixinStringBuilder(
            if (parsedVersion > "1.21.1") {
                "bodyKneesAndToes.EquipmentRenderMixin" 
            }
            else {
                "bodyKneesAndToes.HumanoidArmorLayerMixin"
            })
            
        if (parsedVersion > "1.21.1") {
            mixinStringBuilder.addMixin("bodyKneesAndToes.ArmorFeatureRenderMixin")
        }

        mixinStringBuilder.addMixin("hand.ItemEntityRendererMixin")
        mixinStringBuilder.addMixin("hand.ItemInHandLayerMixin")
        mixinStringBuilder.addMixin("hand.OffHandRenderMixin")
        
        if (parsedVersion >= "1.21.9") {
            mixinStringBuilder.addMixin("hand.ItemRenderStateMixin")
            mixinStringBuilder.addMixin("hand.SubmitNodeCollectorMixin")
        }
        else {
            mixinStringBuilder.addMixin("hand.ModelPartMixin")
            mixinStringBuilder.addMixin("hand.ItemRendererMixin")
        }
        
        
        return if (parsedVersion >= "1.20.5") {
            mixinStringBuilder.getMixinString("networking.ClientPacketListenerMixin")
        } else {
            mixinStringBuilder.getMixinString("networking.ClientPlayNetworkHandlerMixin")
        }
    }
}