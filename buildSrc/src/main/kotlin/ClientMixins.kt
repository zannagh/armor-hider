import dev.kikugie.stonecutter.data.ParsedVersion

class ClientMixins(
    val parsedVersion: ParsedVersion,
    val loader: String = "fabric"
) {
    
    fun getScreenMixinString(): String {
        // For 1.20.x: Use OptionsScreenMixin (injects into main options screen)
        // For 1.21+: Use SkinOptionsMixin (injects into skin options screen)
        return if (parsedVersion >= "1.21.9") {
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
            // NeoForge patches renderLayers differently — getColorForLayer is never invoked.
            // The color mixin only works on Fabric.
            if (loader != "neoforge") {
                mixinStringBuilder.addMixin("bodyKneesAndToes.EquipmentRenderColorMixin")
            }
        }

        mixinStringBuilder.addMixin("hand.ItemEntityRendererMixin")
        mixinStringBuilder.addMixin("hand.ItemInHandLayerMixin")
        mixinStringBuilder.addMixin("hand.OffHandRenderMixin")
        
        if (parsedVersion >= "1.21.9") {
            mixinStringBuilder.addMixin("hand.ItemRenderStateMixin")
            mixinStringBuilder.addMixin("hand.SubmitNodeCollectorMixin")
            // NeoForge: armor color transparency via SubmitNodeCollection (same level as offhand)
            // since getColorForLayer is patched out of renderLayers on NeoForge
            if (loader == "neoforge") {
                mixinStringBuilder.addMixin("bodyKneesAndToes.NeoForgeArmorColorMixin")
            }
        }
        else {
            mixinStringBuilder.addMixin("hand.ModelPartMixin")
            // NeoForge patches ItemRenderer.renderQuadList — putBulkData is never invoked.
            if (loader != "neoforge") {
                mixinStringBuilder.addMixin("hand.ItemRendererMixin")
            }
        }
        
        
        if (parsedVersion >= "1.20.5") {
            // NeoForge: payload dispatch + client join handled natively via events
            if (loader != "neoforge") {
                mixinStringBuilder.addMixin("networking.ClientPacketListenerMixin")
            }
            return mixinStringBuilder.getMixinString()
        } else {
            return mixinStringBuilder.getMixinString("networking.ClientPlayNetworkHandlerMixin")
        }
    }
}