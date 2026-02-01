//? if >= 1.20.5 {
package de.zannagh.armorhider.mixin.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.ArmorHiderPayloadList;
import de.zannagh.armorhider.net.PayloadRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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

        var c2sPackets = PayloadRegistry.getAllC2S();
        var s2cPackets = PayloadRegistry.getAllS2C();

        if (types instanceof ArmorHiderPayloadList) {
            return types;
        }

        boolean alreadyWrappedByOtherMod = types.stream()
                .anyMatch(tac -> c2sPackets.containsKey(tac.type().id()) || s2cPackets.containsKey(tac.type().id()));
        if (alreadyWrappedByOtherMod) {
            return types;
        }

        // Create our marker wrapper and add custom payload types
        ArmorHiderPayloadList<CustomPacketPayload.TypeAndCodec<? super B, ?>> modifiedTypes = new ArmorHiderPayloadList<>(types);

        ArmorHider.LOGGER.info("Injecting custom payloads into codec. Current types: {}, adding C2S: {}, S2C: {}",
                types.size(), c2sPackets.size(), s2cPackets.size());

        c2sPackets.forEach((id, entry) -> {
            var typeAndCodec = new CustomPacketPayload.TypeAndCodec(entry.type(), entry.codec());
            modifiedTypes.add(typeAndCodec);
            ArmorHider.LOGGER.info("Injected C2S payload: {}", id);
        });

        s2cPackets.forEach((id, entry) -> {
            var typeAndCodec = new CustomPacketPayload.TypeAndCodec(entry.type(), entry.codec());
            modifiedTypes.add(typeAndCodec);
            ArmorHider.LOGGER.info("Injected S2C payload: {}", id);
        });

        return modifiedTypes;
    }
}
//?}
