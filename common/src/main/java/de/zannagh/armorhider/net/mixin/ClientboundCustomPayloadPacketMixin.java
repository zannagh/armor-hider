//? if >= 1.20.5 {
package de.zannagh.armorhider.net.mixin;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.ArmorHiderPayloadList;
import de.zannagh.armorhider.net.PayloadRegistry;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

/**
 * Injects S2C (server-to-client) payload types into the codec created in
 * {@link ClientboundCustomPayloadPacket}'s static initializer.
 * <p>
 * Targets the <em>caller</em> of {@code CustomPacketPayload.codec()} rather than
 * the method itself, avoiding the "target loaded too early" crash that occurs when
 * another mod (e.g. Vivecraft) classloads {@code CustomPacketPayload} during boot.
 */
@Mixin(ClientboundCustomPayloadPacket.class)
public class ClientboundCustomPayloadPacketMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @ModifyArg(
            method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;"),
            index = 1
    )
    private static List<CustomPacketPayload.TypeAndCodec<?, ?>> injectS2CPayloads(
            List<CustomPacketPayload.TypeAndCodec<?, ?>> types) {
        if (types instanceof ArmorHiderPayloadList) return types;

        var s2cPackets = PayloadRegistry.getAllS2C();
        if (types.stream().anyMatch(tac -> s2cPackets.containsKey(tac.type().id()))) {
            return types;
        }

        ArmorHiderPayloadList<CustomPacketPayload.TypeAndCodec<?, ?>> modifiedTypes = new ArmorHiderPayloadList<>(types);
        ArmorHider.LOGGER.info("Injecting S2C payloads into ClientboundCustomPayloadPacket codec. Current types: {}, adding: {}",
                types.size(), s2cPackets.size());
        s2cPackets.forEach((id, entry) -> {
            modifiedTypes.add(new CustomPacketPayload.TypeAndCodec(entry.type(), entry.codec()));
            ArmorHider.LOGGER.info("Injected S2C payload: {}", id);
        });
        return modifiedTypes;
    }
}
//?}
