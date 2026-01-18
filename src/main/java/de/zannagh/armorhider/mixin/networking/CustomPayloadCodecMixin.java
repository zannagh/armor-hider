package de.zannagh.armorhider.mixin.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.PayloadRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to inject our custom payload types into the vanilla codec system.
 * This intercepts the codec creation and adds our registered payloads to the types list.
 */
@Mixin(CustomPacketPayload.class)
public interface CustomPayloadCodecMixin {

    /**
     * Modify the types list parameter to include our custom payloads.
     * This targets the List parameter at index 1 (after FallbackProvider).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @ModifyVariable(
            method = "codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private static <B extends FriendlyByteBuf> List<CustomPacketPayload.TypeAndCodec<? super B, ?>> modifyPayloadTypes(
            List<CustomPacketPayload.TypeAndCodec<? super B, ?>> types) {

        // Create a mutable copy and add our payload types
        List<CustomPacketPayload.TypeAndCodec<? super B, ?>> modifiedTypes = new ArrayList<>(types);

        ArmorHider.LOGGER.info("Injecting custom payloads into codec. Current types: {}, adding C2S: {}, S2C: {}",
                types.size(), PayloadRegistry.getAllC2S().size(), PayloadRegistry.getAllS2C().size());

        // Add our C2S payloads
        PayloadRegistry.getAllC2S().forEach((id, entry) -> {
            var typeAndCodec = new CustomPacketPayload.TypeAndCodec(entry.type(), entry.codec());
            modifiedTypes.add(typeAndCodec);
            ArmorHider.LOGGER.info("Injected C2S payload: {}", id);
        });

        // Add our S2C payloads
        PayloadRegistry.getAllS2C().forEach((id, entry) -> {
            var typeAndCodec = new CustomPacketPayload.TypeAndCodec(entry.type(), entry.codec());
            modifiedTypes.add(typeAndCodec);
            ArmorHider.LOGGER.info("Injected S2C payload: {}", id);
        });

        return modifiedTypes;
    }
}
