//? if >= 1.20.5 {
package de.zannagh.armorhider.configuration;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Marker interface for configuration classes that should have their
// ConfigurationItemBase fields automatically initialized when missing from JSON.
public interface ConfigurationSource<T> extends CustomPacketPayload {

    StreamCodec<ByteBuf, T> getCodec();

    // Indicates whether the configuration source has been altered compared to the
    // state derived from its serialized content, such as JSON representation.
    boolean hasChangedFromSerializedContent();

    // Marks the configuration source as having been modified from its serialized state.
    void setHasChangedFromSerializedContent();
}
//?}

//? if < 1.20.5 {
/*package de.zannagh.armorhider.configuration;

// Marker interface for configuration classes that should have their
// ConfigurationItemBase fields automatically initialized when missing from JSON.
// In 1.20.x, this does not extend CustomPacketPayload as that interface doesn't exist.
public interface ConfigurationSource<T> {

    // Indicates whether the configuration source has been altered compared to the
    // state derived from its serialized content, such as JSON representation.
    boolean hasChangedFromSerializedContent();

    // Marks the configuration source as having been modified from its serialized state.
    void setHasChangedFromSerializedContent();
}
*///?}
