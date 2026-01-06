package de.zannagh.armorhider.configuration;

import de.zannagh.armorhider.resources.PlayerConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Marker interface for configuration classes that should have their
 * ConfigurationItemBase fields automatically initialized when missing from JSON.
 */
public interface ConfigurationSource<T> extends CustomPayload {
    
    PacketCodec<ByteBuf, T> getCodec();
    
    /**
     * Indicates whether the configuration source has been altered compared to the
     * state derived from its serialized content, such as JSON representation.
     *
     * @return true if the configuration has been modified from its serialized state;
     *         false otherwise.
     */
    boolean hasChangedFromSerializedContent();

    /**
     * Marks the configuration source as having been modified from its serialized state.
     * This method is intended to be used to indicate that the state of a configuration
     * source has been altered since it was last loaded or serialized from a source such
     * as its JSON representation. This can be used in conjunction with 
     * {@link #hasChangedFromSerializedContent()} to track changes and determine if
     * re-serialization or saving is required.
     */
    void setHasChangedFromSerializedContent();
}
