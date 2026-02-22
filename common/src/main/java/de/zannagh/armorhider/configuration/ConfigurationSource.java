package de.zannagh.armorhider.configuration;

//? if >= 1.20.5 {
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? }

// Marker interface for configuration classes that should have their
// ConfigurationItemBase fields automatically initialized when missing from JSON.
// In < 1.20.5 the CustomPacketPayload is not yet existant, so the imports and extends are not needed.
public interface ConfigurationSource<T>
    //? if >= 1.20.5
    extends CustomPacketPayload 
{

    //? if >= 1.20.5
    StreamCodec<ByteBuf, T> getCodec();

    // Indicates whether the configuration source has been altered compared to the
    // state derived from its serialized content, such as JSON representation.
    boolean hasChangedFromSerializedContent();

    // Marks the configuration source as having been modified from its serialized state.
    void setHasChangedFromSerializedContent();
}