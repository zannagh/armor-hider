//? if >= 1.20.5 {
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

@Mixin(CustomPacketPayload.class)
public interface CustomPayloadCodecMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @ModifyVariable(
            method = "codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private static <B extends FriendlyByteBuf> List<CustomPacketPayload.TypeAndCodec<? super B, ?>> modifyPayloadTypes(
            List<CustomPacketPayload.TypeAndCodec<? super B, ?>> types) {

        // Create a mutable copy and add custom payload types
        List<CustomPacketPayload.TypeAndCodec<? super B, ?>> modifiedTypes = new ArrayList<>(types);

        ArmorHider.LOGGER.info("Injecting custom payloads into codec. Current types: {}, adding C2S: {}, S2C: {}",
                types.size(), PayloadRegistry.getAllC2S().size(), PayloadRegistry.getAllS2C().size());

        PayloadRegistry.getAllC2S().forEach((id, entry) -> {
            var typeAndCodec = new CustomPacketPayload.TypeAndCodec(entry.type(), entry.codec());
            modifiedTypes.add(typeAndCodec);
            ArmorHider.LOGGER.info("Injected C2S payload: {}", id);
        });

        PayloadRegistry.getAllS2C().forEach((id, entry) -> {
            var typeAndCodec = new CustomPacketPayload.TypeAndCodec(entry.type(), entry.codec());
            modifiedTypes.add(typeAndCodec);
            ArmorHider.LOGGER.info("Injected S2C payload: {}", id);
        });

        return modifiedTypes;
    }
}
//?}
